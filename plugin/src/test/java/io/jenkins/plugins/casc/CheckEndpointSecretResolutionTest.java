package io.jenkins.plugins.casc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.oneOf;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import jenkins.model.Jenkins;
import org.htmlunit.HttpMethod;
import org.htmlunit.WebRequest;
import org.htmlunit.WebResponse;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.TestExtension;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Ensures that {@code /configuration-as-code/check} never resolves variables for callers without
 * {@link Jenkins#ADMINISTER}, so resolved values cannot be disclosed through validation output.
 */
@WithJenkins
class CheckEndpointSecretResolutionTest {

    private static final String CHECK_ENDPOINT = "configuration-as-code/check";
    private static final String ADMIN = "admin";
    private static final String SYSTEM_READER = "systemReader";
    private static final String READER = "reader";
    private static final String ENV_VAR = "CASC_CHECK_ENDPOINT_TEST_MARKER";
    private static final String SYS_PROP = "casc.check.endpoint.test.marker";

    private JenkinsRule j;
    private boolean systemReadWasEnabled;
    private final EnvironmentVariables environment = new EnvironmentVariables();

    private final String fileMarker = "file-marker-" + UUID.randomUUID();
    private final String envMarker = "env-marker-" + UUID.randomUUID();
    private final String sysPropMarker = "sysprop-marker-" + UUID.randomUUID();
    private final String secretSourceMarker = "secret-source-marker-" + UUID.randomUUID();
    private Path secretFile;
    /**
     * Path of {@link #secretFile} as embedded in YAML. Forward slashes so Windows backslashes are not treated as
     * escape sequences in double-quoted YAML scalars (or escaped in the JSON response); Java accepts them on Windows.
     */
    private String secretPath;

    @BeforeEach
    void setUp(JenkinsRule j, @TempDir Path tempDir) throws Exception {
        this.j = j;
        systemReadWasEnabled = Jenkins.SYSTEM_READ.getEnabled();
        Jenkins.SYSTEM_READ.setEnabled(true);

        secretFile = tempDir.resolve("secret.txt");
        Files.writeString(secretFile, fileMarker, StandardCharsets.UTF_8);
        secretPath = secretFile.toAbsolutePath().toString().replace('\\', '/');
        environment.set(ENV_VAR, envMarker);
        System.setProperty(SYS_PROP, sysPropMarker);
        RecordingSecretSource.get().reset(secretSourceMarker);

        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Jenkins.ADMINISTER)
                .everywhere()
                .to(ADMIN)
                .grant(Jenkins.READ, Jenkins.SYSTEM_READ)
                .everywhere()
                .to(SYSTEM_READER)
                .grant(Jenkins.READ)
                .everywhere()
                .to(READER));
    }

    @AfterEach
    void tearDown() {
        Jenkins.SYSTEM_READ.setEnabled(systemReadWasEnabled);
        environment.set(ENV_VAR, null);
        System.clearProperty(SYS_PROP);
    }

    private Map<String, String> expressions() {
        return Map.of(
                "${readFile:" + secretPath + "}", fileMarker,
                "${fileBase64:" + secretPath + "}", fileMarker,
                "${decodeBase64:${fileBase64:" + secretPath + "}}", fileMarker,
                "${" + ENV_VAR + "}", envMarker,
                "${sysProp:" + SYS_PROP + "}", sysPropMarker,
                "${" + RecordingSecretSource.KEY + "}", secretSourceMarker);
    }

    private static List<String> yamlTemplates() {
        return List.of(
                // symbol position: resolved value would be echoed in "No ... implementation found for <value>"
                "jenkins:\n  securityRealm: \"%s\"\n",
                // numeric field: resolved value would be echoed by NumberFormatException
                "jenkins:\n  numExecutors: \"%s\"\n",
                // plain string field
                "jenkins:\n  systemMessage: \"%s\"\n",
                // list of describables in symbol position
                "jenkins:\n  nodeProperties:\n    - \"%s\"\n");
    }

    private WebResponse check(String user, String yaml) throws Exception {
        try (JenkinsRule.WebClient wc =
                j.createWebClient().withBasicApiToken(user).withThrowExceptionOnFailingStatusCode(false)) {
            WebRequest request = new WebRequest(new URL(j.getURL(), CHECK_ENDPOINT), HttpMethod.POST);
            request.setAdditionalHeader("Content-Type", "application/x-yaml");
            request.setRequestBody(yaml);
            return wc.getPage(request).getWebResponse();
        }
    }

    @Test
    void systemReaderNeverReceivesResolvedValues() throws Exception {
        for (String template : yamlTemplates()) {
            for (Map.Entry<String, String> expression : expressions().entrySet()) {
                String yaml = String.format(template, expression.getKey());
                WebResponse response = check(SYSTEM_READER, yaml);
                String body = response.getContentAsString();

                assertThat(yaml, response.getStatusCode(), is(oneOf(200, 400)));
                assertThat(yaml, body, not(containsString(expression.getValue())));
                // base64 of the file content must not leak either
                assertThat(
                        yaml,
                        body,
                        not(containsString(
                                Base64.getEncoder().encodeToString(fileMarker.getBytes(StandardCharsets.UTF_8)))));
            }
        }
        assertThat(
                "secret sources must not be consulted for non-administrators",
                RecordingSecretSource.get().revealed,
                is(empty()));
    }

    @Test
    void systemReaderSeesUnresolvedExpression() throws Exception {
        String expression = "${readFile:" + secretPath + "}";
        WebResponse response = check(SYSTEM_READER, "jenkins:\n  securityRealm: \"" + expression + "\"\n");

        assertThat(response.getStatusCode(), is(400));
        assertThat(response.getContentAsString(), containsString(expression));
        assertThat(response.getContentAsString(), not(containsString(fileMarker)));
    }

    @Test
    void administratorStillGetsRealResolution() throws Exception {
        WebResponse response = check(ADMIN, "jenkins:\n  securityRealm: \"${readFile:" + secretPath + "}\"\n");
        assertThat(response.getStatusCode(), is(400));
        assertThat(response.getContentAsString(), containsString(fileMarker));

        response = check(ADMIN, "jenkins:\n  securityRealm: \"${" + RecordingSecretSource.KEY + "}\"\n");
        assertThat(response.getContentAsString(), containsString(secretSourceMarker));
        assertThat(RecordingSecretSource.get().revealed, hasItem(RecordingSecretSource.KEY));

        response = check(ADMIN, "jenkins:\n  securityRealm: \"${sysProp:" + SYS_PROP + "}\"\n");
        assertThat(response.getContentAsString(), containsString(sysPropMarker));
    }

    @Test
    void userWithoutSystemReadIsForbidden() throws Exception {
        WebResponse response = check(READER, "jenkins:\n  securityRealm: \"${readFile:" + secretPath + "}\"\n");
        assertThat(response.getStatusCode(), is(403));
        assertThat(response.getContentAsString(), not(containsString(fileMarker)));
    }

    @Test
    void contextWithResolutionDisabledNeverResolves() {
        ConfigurationContext context = new ConfigurationContext(ConfiguratorRegistry.get());
        context.disableSecretResolution();

        assertThat(context.isSecretResolutionEnabled(), is(false));
        assertThat(context.getSecretSources(), is(empty()));
        for (String expression : expressions().keySet()) {
            assertThat(context.getSecretSourceResolver().resolve(expression), is(expression));
            assertThat(context.getSecretSourceResolver().resolve("prefix " + expression), is("prefix " + expression));
        }
        assertThat(RecordingSecretSource.get().revealed, is(empty()));

        ConfigurationContext enabled = new ConfigurationContext(ConfiguratorRegistry.get());
        assertThat(enabled.isSecretResolutionEnabled(), is(true));
        assertThat(enabled.getSecretSourceResolver().resolve("${readFile:" + secretPath + "}"), is(fileMarker));
    }

    @TestExtension
    public static class RecordingSecretSource extends SecretSource {

        static final String KEY = "CASC_CHECK_ENDPOINT_RECORDING_SECRET";

        final List<String> revealed = new ArrayList<>();
        private String value;

        static RecordingSecretSource get() {
            return Jenkins.get().getExtensionList(SecretSource.class).get(RecordingSecretSource.class);
        }

        void reset(String value) {
            this.value = value;
            revealed.clear();
        }

        @Override
        public Optional<String> reveal(String secret) {
            revealed.add(secret);
            if (KEY.equals(secret)) {
                return Optional.of(value);
            }
            return Optional.empty();
        }
    }
}
