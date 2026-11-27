package io.jenkins.plugins.casc;

import static com.nimbusds.jose.EncryptionMethod.A256GCM;
import static com.nimbusds.jose.JWEAlgorithm.RSA_OAEP_256;
import static io.jenkins.plugins.casc.impl.JweEncryptionEngine.KEY_ENV;
import static io.jenkins.plugins.casc.impl.JweEncryptionEngine.KEY_PROP;
import static java.lang.System.clearProperty;
import static java.lang.System.getProperty;
import static java.lang.System.setProperty;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.isReadable;
import static java.nio.file.Files.writeString;
import static java.util.Arrays.copyOf;
import static java.util.Base64.getMimeEncoder;
import static java.util.Base64.getUrlEncoder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSAEncrypter;
import hudson.ProxyConfiguration;
import hudson.util.Secret;
import io.jenkins.plugins.casc.core.CNodeInterpolator;
import io.jenkins.plugins.casc.encryption.DecryptionRootConfigurator;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import io.jenkins.plugins.casc.model.Scalar;
import io.jenkins.plugins.casc.model.Source;
import io.jenkins.plugins.casc.yaml.YamlSource;
import io.jenkins.plugins.casc.yaml.YamlUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.List;
import java.util.Map;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import org.htmlunit.HttpMethod;
import org.htmlunit.WebRequest;
import org.htmlunit.WebResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@WithJenkins
@ExtendWith(SystemStubsExtension.class)
class JweYamlConfigurationIntegrationTest {

    @SystemStub
    private final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @TempDir
    private Path tempDir;

    private String originalProp;

    @BeforeEach
    void captureAndClearKeyConfig() {
        originalProp = getProperty(KEY_PROP);
        clearProperty(KEY_PROP);
        environmentVariables.set(KEY_ENV, null);
    }

    @AfterEach
    void restoreKeyConfig() {
        if (originalProp == null) {
            clearProperty(KEY_PROP);
        } else {
            setProperty(KEY_PROP, originalProp);
        }
    }

    @Test
    void configureDecryptsJweFromYamlRegardlessOfSectionOrderAndOmitsKeyFromExport(JenkinsRule j) throws Exception {
        RsaFixture fixture = new RsaFixture(tempDir, "primary.pem");
        String secretFirst = "yaml-first-secret";
        String secretAfter = "yaml-after-secret";

        String yamlDecryptionFirst = """
                decryption:
                  jwe:
                    keyFile: "%s"
                jenkins:
                  systemMessage: "${decrypt:jwe:%s}"
                """.formatted(yamlPath(fixture.keyFile), fixture.encrypt(secretFirst));

        Path configBefore = writeYaml("decryption-first.yaml", yamlDecryptionFirst);
        ConfigurationAsCode.get().configure(configBefore.toUri().toString());
        assertThat(j.jenkins.getSystemMessage(), equalTo(secretFirst));

        String yamlJenkinsFirst = """
                jenkins:
                  systemMessage: "${decrypt:jwe:%s}"
                decryption:
                  jwe:
                    keyFile: "%s"
                """.formatted(fixture.encrypt(secretAfter), yamlPath(fixture.keyFile));

        Path configAfter = writeYaml("jenkins-first.yaml", yamlJenkinsFirst);
        ConfigurationAsCode.get().configure(configAfter.toUri().toString());
        assertThat(j.jenkins.getSystemMessage(), equalTo(secretAfter));

        ByteArrayOutputStream exportStream = new ByteArrayOutputStream();
        ConfigurationAsCode.get().export(exportStream);
        String exported = exportStream.toString(UTF_8);
        assertThat(exported, containsString(secretAfter));
        assertThat(exported, not(containsString("decryption:")));
        assertThat(exported, not(containsString(yamlPath(fixture.keyFile))));
    }

    @Test
    void checkWithAndCheckEndpointResolveYamlKeyBeforeValidationAndProtectNonAdmins(JenkinsRule j) throws Exception {
        boolean systemReadWasEnabled = Jenkins.SYSTEM_READ.getEnabled();
        Jenkins.SYSTEM_READ.setEnabled(true);
        try {
            RsaFixture fixture = new RsaFixture(tempDir, "check-endpoint.pem");
            String encryptedFormatter = fixture.encrypt("rawHtml");
            String encryptedExecutors = fixture.encrypt("3");
            String encryptedMissingRealm = fixture.encrypt("missing-realm-from-jwe");

            String validYaml = """
                    jenkins:
                      markupFormatter: "${decrypt:jwe:%s}"
                      numExecutors: "${decrypt:jwe:%s}"
                    decryption:
                      jwe:
                        keyFile: "%s"
                    """.formatted(encryptedFormatter, encryptedExecutors, yamlPath(fixture.keyFile));

            Path validConfig = writeYaml("check-valid.yaml", validYaml);
            Map<Source, String> issues = ConfigurationAsCode.get().checkWith(YamlSource.of(validConfig));
            assertThat(issues.entrySet(), is(empty()));

            j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
            j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                    .grant(Jenkins.ADMINISTER)
                    .everywhere()
                    .to("admin")
                    .grant(Jenkins.READ, Jenkins.SYSTEM_READ)
                    .everywhere()
                    .to("systemReader"));

            WebResponse adminOk = postCheck(j, "admin", validYaml);
            assertThat(adminOk.getStatusCode(), is(200));
            assertThat(JSONArray.fromObject(adminOk.getContentAsString()).size(), is(0));

            String invalidRealmYaml = """
                    jenkins:
                      securityRealm: "${decrypt:jwe:%s}"
                    decryption:
                      jwe:
                        keyFile: "%s"
                    """.formatted(encryptedMissingRealm, yamlPath(fixture.keyFile));
            WebResponse adminBadRealm = postCheck(j, "admin", invalidRealmYaml);
            assertThat(adminBadRealm.getStatusCode(), is(400));
            assertThat(adminBadRealm.getContentAsString(), containsString("missing-realm-from-jwe"));

            String secretMessage = "super-secret-check-endpoint-plaintext";
            String readerExpression = "${decrypt:jwe:" + fixture.encrypt(secretMessage) + "}";
            String readerYaml = """
                    decryption:
                      jwe:
                        keyFile: "%s"
                    jenkins:
                      securityRealm: "%s"
                    """.formatted(yamlPath(tempDir.resolve("never-read-missing.pem")), readerExpression);

            WebResponse readerResponse = postCheck(j, "systemReader", readerYaml);
            assertThat(readerResponse.getStatusCode(), is(400));
            assertThat(readerResponse.getContentAsString(), containsString(readerExpression));
            assertThat(readerResponse.getContentAsString(), not(containsString(secretMessage)));
            assertThat(readerResponse.getContentAsString(), not(containsString("does not exist")));
        } finally {
            Jenkins.SYSTEM_READ.setEnabled(systemReadWasEnabled);
        }
    }

    @Test
    void configureMergesMultipleYamlSourcesAndHonorsYamlUnknownWarnPolicy(JenkinsRule j) throws Exception {
        RsaFixture fixture = new RsaFixture(tempDir, "merged.pem");
        String secret = "merged-source-secret";

        String decryptionAndPolicyYaml = """
                configuration-as-code:
                  unknown: warn
                decryption:
                  unknownTopAttr: tolerated-under-warn
                  jwe:
                    keyFile: "%s"
                    unknownNestedAttr: tolerated-under-warn
                """.formatted(yamlPath(fixture.keyFile));

        String jenkinsYaml = """
                jenkins:
                  systemMessage: "${decrypt:jwe:%s}"
                """.formatted(fixture.encrypt(secret));

        Path source1 = writeYaml("01-decryption.yaml", decryptionAndPolicyYaml);
        Path source2 = writeYaml("02-jenkins.yaml", jenkinsYaml);

        ConfigurationAsCode.get()
                .configure(source1.toUri().toString(), source2.toUri().toString());
        assertThat(j.jenkins.getSystemMessage(), equalTo(secret));

        String strictYaml = """
                decryption:
                  jwe:
                    keyFile: "%s"
                    unknownNestedAttr: rejected
                """.formatted(yamlPath(fixture.keyFile));
        Path strictFile = writeYaml("strict-unknown.yaml", strictYaml);
        assertThrows(UnknownAttributesException.class, () -> ConfigurationAsCode.get()
                .configure(strictFile.toUri().toString()));
    }

    @Test
    void configureRespectsKeySourcePrecedenceSystemPropertyOverEnvOverYaml(JenkinsRule j) throws Exception {
        RsaFixture propFixture = new RsaFixture(tempDir, "prop.pem");
        RsaFixture envFixture = new RsaFixture(tempDir, "env.pem");
        RsaFixture yamlFixture = new RsaFixture(tempDir, "yaml.pem");

        String propSecret = "prop-wins";
        String envSecret = "env-wins";
        String yamlSecret = "yaml-wins";

        setProperty(KEY_PROP, propFixture.keyFile.toAbsolutePath().toString());
        environmentVariables.set(KEY_ENV, envFixture.keyFile.toAbsolutePath().toString());

        configureWithMessage(yamlFixture.keyFile, "${decrypt:jwe:" + propFixture.encrypt(propSecret) + "}");
        assertThat(j.jenkins.getSystemMessage(), equalTo(propSecret));

        assertThrows(
                ConfiguratorException.class,
                () -> configureWithMessage(
                        yamlFixture.keyFile, "${decrypt:jwe:" + envFixture.encrypt(envSecret) + "}"));
        assertThrows(
                ConfiguratorException.class,
                () -> configureWithMessage(
                        yamlFixture.keyFile, "${decrypt:jwe:" + yamlFixture.encrypt(yamlSecret) + "}"));

        clearProperty(KEY_PROP);
        configureWithMessage(yamlFixture.keyFile, "${decrypt:jwe:" + envFixture.encrypt(envSecret) + "}");
        assertThat(j.jenkins.getSystemMessage(), equalTo(envSecret));

        assertThrows(
                ConfiguratorException.class,
                () -> configureWithMessage(
                        yamlFixture.keyFile, "${decrypt:jwe:" + yamlFixture.encrypt(yamlSecret) + "}"));

        environmentVariables.set(KEY_ENV, null);
        configureWithMessage(yamlFixture.keyFile, "${decrypt:jwe:" + yamlFixture.encrypt(yamlSecret) + "}");
        assertThat(j.jenkins.getSystemMessage(), equalTo(yamlSecret));
    }

    @Test
    @SuppressWarnings("unused")
    void invalidOrMissingKeyConfigurationFailsClearlyWithoutFallbackInJcasc(JenkinsRule j) throws Exception {
        RsaFixture validFixture = new RsaFixture(tempDir, "valid.pem");
        String ciphertext = validFixture.encrypt("never-decrypted");

        Path missingFile = tempDir.resolve("missing.pem");
        ConfiguratorException missingEx = assertThrows(
                ConfiguratorException.class,
                () -> configureWithMessage(missingFile, "${decrypt:jwe:" + ciphertext + "}"));
        assertTrue(
                hasMessageInCauseChain(missingEx, "does not exist"),
                "Missing YAML key file error should preserve clear cause");

        Path malformedPem = tempDir.resolve("malformed.pem");
        writeString(malformedPem, "not a private key PEM", UTF_8);
        ConfiguratorException malformedEx = assertThrows(
                ConfiguratorException.class,
                () -> configureWithMessage(malformedPem, "${decrypt:jwe:" + ciphertext + "}"));
        assertTrue(
                hasMessageInCauseChain(malformedEx, "Failed to parse"),
                "Invalid YAML key material should preserve clear parse cause");

        setProperty(KEY_PROP, malformedPem.toAbsolutePath().toString());
        environmentVariables.set(KEY_ENV, validFixture.keyFile.toAbsolutePath().toString());
        ConfiguratorException badPropEx = assertThrows(
                ConfiguratorException.class,
                () -> configureWithMessage(validFixture.keyFile, "${decrypt:jwe:" + ciphertext + "}"));
        assertTrue(hasMessageInCauseChain(badPropEx, "Failed to parse"));

        clearProperty(KEY_PROP);
        environmentVariables.set(KEY_ENV, malformedPem.toAbsolutePath().toString());
        ConfiguratorException badEnvEx = assertThrows(
                ConfiguratorException.class,
                () -> configureWithMessage(validFixture.keyFile, "${decrypt:jwe:" + ciphertext + "}"));
        assertTrue(hasMessageInCauseChain(badEnvEx, "Failed to parse"));

        environmentVariables.set(KEY_ENV, null);
        Path noDecryptionYaml = writeYaml(
                "missing-decryption.yaml", "jenkins:\n  systemMessage: \"${decrypt:jwe:" + ciphertext + "}\"\n");
        ConfiguratorException noSourceEx = assertThrows(ConfiguratorException.class, () -> ConfigurationAsCode.get()
                .configure(noDecryptionYaml.toUri().toString()));
        assertTrue(
                hasMessageInCauseChain(noSourceEx, "not configured"),
                "Missing decryption key source should preserve clear configuration cause");

        Path unreadableFile = tempDir.resolve("unreadable-casc.pem");
        writeString(unreadableFile, Files.readString(validFixture.keyFile, UTF_8), UTF_8);
        try {
            boolean permissionsChanged = unreadableFile.toFile().setReadable(false, false);
            assumeTrue(
                    permissionsChanged && !isReadable(unreadableFile),
                    "Skipped: could not revoke file read permission (running as root?)");

            ConfiguratorException unreadableEx = assertThrows(
                    ConfiguratorException.class,
                    () -> configureWithMessage(unreadableFile, "${decrypt:jwe:" + ciphertext + "}"));
            assertTrue(
                    hasMessageInCauseChain(unreadableEx, "not readable"),
                    "Unreadable YAML key file should preserve clear permission cause");
        } finally {
            if (!unreadableFile.toFile().setReadable(true, true)) {
                unreadableFile.toFile().deleteOnExit();
            }
        }
    }

    @Test
    void yamlKeyPathDoesNotLeakBetweenContextsOrSequentialConfigurations(JenkinsRule j) throws Exception {
        RsaFixture firstFixture = new RsaFixture(tempDir, "first.pem");
        RsaFixture secondFixture = new RsaFixture(tempDir, "second.pem");

        String firstCiphertext = firstFixture.encrypt("first-secret");
        String secondCiphertext = secondFixture.encrypt("second-secret");

        configureWithMessage(firstFixture.keyFile, "${decrypt:jwe:" + firstCiphertext + "}");
        assertThat(j.jenkins.getSystemMessage(), equalTo("first-secret"));

        configureWithMessage(secondFixture.keyFile, "${decrypt:jwe:" + secondCiphertext + "}");
        assertThat(j.jenkins.getSystemMessage(), equalTo("second-secret"));

        String failingYaml = """
                decryption:
                  jwe:
                    keyFile: "%s"
                jenkins:
                  securityRealm: "non-existent-realm"
                """.formatted(yamlPath(firstFixture.keyFile));
        Path failingFile = writeYaml("failing-followup.yaml", failingYaml);
        assertThrows(
                ConfiguratorException.class, () -> ConfigurationAsCode.get().checkWith(YamlSource.of(failingFile)));

        Path noKeyYaml = writeYaml(
                "no-decryption-followup.yaml",
                "jenkins:\n  systemMessage: \"${decrypt:jwe:" + firstCiphertext + "}\"\n");
        assertThrows(ConfiguratorException.class, () -> ConfigurationAsCode.get()
                .configure(noKeyYaml.toUri().toString()));

        ConfigurationContext ctx1 = new ConfigurationContext(ConfiguratorRegistry.get());
        DecryptionRootConfigurator.initialize(
                loadYaml("decryption:\n  jwe:\n    keyFile: \"" + yamlPath(firstFixture.keyFile) + "\"\n", ctx1), ctx1);

        ConfigurationContext ctx2 = new ConfigurationContext(ConfiguratorRegistry.get());
        DecryptionRootConfigurator.initialize(
                loadYaml("decryption:\n  jwe:\n    keyFile: \"" + yamlPath(secondFixture.keyFile) + "\"\n", ctx2),
                ctx2);

        ConfigurationContext unconfiguredCtx = new ConfigurationContext(ConfiguratorRegistry.get());

        CNode interpolated1 = CNodeInterpolator.interpolate(new Scalar("${decrypt:jwe:" + firstCiphertext + "}"), ctx1);
        CNode interpolated2 =
                CNodeInterpolator.interpolate(new Scalar("${decrypt:jwe:" + secondCiphertext + "}"), ctx2);
        assertThat(interpolated1.asScalar().getValue(), equalTo("first-secret"));
        assertThat(interpolated2.asScalar().getValue(), equalTo("second-secret"));

        assertThrows(IllegalStateException.class, () -> ctx1.getSecretSourceResolver()
                .resolve("${decrypt:jwe:" + secondCiphertext + "}"));
        assertThrows(IllegalStateException.class, () -> ctx2.getSecretSourceResolver()
                .resolve("${decrypt:jwe:" + firstCiphertext + "}"));
        assertThrows(
                IllegalStateException.class,
                () -> unconfiguredCtx.getSecretSourceResolver().resolve("${decrypt:jwe:" + firstCiphertext + "}"));
    }

    @Test
    void backwardCompatibilityPropEnvExistingSecretsAndJenkinsAqSecretsRemainUnaffected(JenkinsRule j)
            throws Exception {
        RsaFixture propFixture = new RsaFixture(tempDir, "compat-prop.pem");
        setProperty(KEY_PROP, propFixture.keyFile.toAbsolutePath().toString());
        Path propOnlyConfig = writeYaml(
                "prop-only.yaml",
                "jenkins:\n  systemMessage: \"${decrypt:jwe:" + propFixture.encrypt("prop-compat-secret") + "}\"\n");
        ConfigurationAsCode.get().configure(propOnlyConfig.toUri().toString());
        assertThat(j.jenkins.getSystemMessage(), equalTo("prop-compat-secret"));

        clearProperty(KEY_PROP);
        RsaFixture envFixture = new RsaFixture(tempDir, "compat-env.pem");
        environmentVariables.set(KEY_ENV, envFixture.keyFile.toAbsolutePath().toString());
        Path envOnlyConfig = writeYaml(
                "env-only.yaml",
                "jenkins:\n  systemMessage: \"${decrypt:jwe:" + envFixture.encrypt("env-compat-secret") + "}\"\n");
        ConfigurationAsCode.get().configure(envOnlyConfig.toUri().toString());
        assertThat(j.jenkins.getSystemMessage(), equalTo("env-compat-secret"));

        environmentVariables.set(KEY_ENV, null);
        environmentVariables.set("COMPAT_PLAIN_ENV", "plain-env-value");

        RsaFixture yamlFixture = new RsaFixture(tempDir, "compat-yaml.pem");
        String aqPlaintext = "jenkins-aq-proxy-secret";
        String aqEncrypted = Secret.fromString(aqPlaintext).getEncryptedValue();
        assertThat(aqEncrypted, startsWith("{AQ"));

        String combinedYaml =
                """
                decryption:
                  jwe:
                    keyFile: "%s"
                jenkins:
                  systemMessage: "${decrypt:jwe:%s}-${COMPAT_PLAIN_ENV}-${UNSET_COMPAT:-fallback}"
                  proxy:
                    name: "proxy.example.internal"
                    port: 8443
                    secretPassword: "%s"
                """.formatted(yamlPath(yamlFixture.keyFile), yamlFixture.encrypt("jwe-compat-part"), aqEncrypted);

        Path combinedFile = writeYaml("combined-compat.yaml", combinedYaml);
        ConfigurationAsCode.get().configure(combinedFile.toUri().toString());

        assertThat(j.jenkins.getSystemMessage(), equalTo("jwe-compat-part-plain-env-value-fallback"));
        ProxyConfiguration proxy = j.jenkins.proxy;
        assertThat(proxy.getSecretPassword().getPlainText(), equalTo(aqPlaintext));

        String jweProxySecret = "jwe-into-hudson-secret";
        String secretFieldYaml = """
                decryption:
                  jwe:
                    keyFile: "%s"
                jenkins:
                  proxy:
                    name: "proxy.example.internal"
                    port: 8443
                    secretPassword: "${decrypt:jwe:%s}"
                """.formatted(yamlPath(yamlFixture.keyFile), yamlFixture.encrypt(jweProxySecret));
        Path secretFieldFile = writeYaml("secret-field-jwe.yaml", secretFieldYaml);
        ConfigurationAsCode.get().configure(secretFieldFile.toUri().toString());

        assertThat(j.jenkins.proxy.getSecretPassword().getPlainText(), equalTo(jweProxySecret));
        assertThat(j.jenkins.proxy.getSecretPassword().getEncryptedValue(), startsWith("{AQ"));

        ByteArrayOutputStream exportOut = new ByteArrayOutputStream();
        ConfigurationAsCode.get().export(exportOut);
        String exported = exportOut.toString(UTF_8);
        assertThat(exported, containsString("{AQ"));
        assertThat(exported, not(containsString(jweProxySecret)));
        assertThat(exported, not(containsString("decryption:")));
    }

    private void configureWithMessage(Path yamlKeyFile, String systemMessageValue) throws Exception {
        String yaml = """
                decryption:
                  jwe:
                    keyFile: "%s"
                jenkins:
                  systemMessage: "%s"
                """.formatted(yamlPath(yamlKeyFile), systemMessageValue);
        Path configFile = createTempFile(tempDir, "casc-config", ".yaml");
        writeString(configFile, yaml, UTF_8);
        ConfigurationAsCode.get().configure(configFile.toUri().toString());
    }

    private Path writeYaml(String fileName, String content) throws Exception {
        Path file = tempDir.resolve(fileName);
        writeString(file, content, UTF_8);
        return file;
    }

    private static Mapping loadYaml(String yaml, ConfigurationContext context) throws ConfiguratorException {
        return YamlUtils.loadFrom(List.of(YamlSource.of(new ByteArrayInputStream(yaml.getBytes(UTF_8)))), context);
    }

    private static WebResponse postCheck(JenkinsRule j, String user, String yaml) throws Exception {
        try (JenkinsRule.WebClient wc =
                j.createWebClient().withBasicApiToken(user).withThrowExceptionOnFailingStatusCode(false)) {
            WebRequest request = new WebRequest(new URL(j.getURL(), "configuration-as-code/check"), HttpMethod.POST);
            request.setAdditionalHeader("Content-Type", "application/x-yaml");
            request.setRequestBody(yaml);
            return wc.getPage(request).getWebResponse();
        }
    }

    private static boolean hasMessageInCauseChain(Throwable error, String expectedSubstring) {
        Throwable current = error;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains(expectedSubstring)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static String yamlPath(Path path) {
        return path.toAbsolutePath().toString().replace('\\', '/');
    }

    private static final class RsaFixture {

        final KeyPair keyPair;
        final Path keyFile;
        final String kid;

        RsaFixture(Path dir, String fileName) throws Exception {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            this.keyPair = generator.generateKeyPair();
            this.keyFile = dir.resolve(fileName);

            String encoded = getMimeEncoder(64, new byte[] {'\n'})
                    .encodeToString(keyPair.getPrivate().getEncoded());
            String pem = "-----BEGIN PRIVATE KEY-----\n" + encoded + "\n-----END PRIVATE KEY-----\n";
            Files.writeString(keyFile, pem, UTF_8);

            RSAPrivateCrtKey crtKey = (RSAPrivateCrtKey) keyPair.getPrivate();
            RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(crtKey.getModulus(), crtKey.getPublicExponent());
            PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(publicKeySpec);
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(publicKey.getEncoded());
            this.kid = getUrlEncoder().withoutPadding().encodeToString(copyOf(hash, 8));
        }

        String encrypt(String plaintext) throws Exception {
            JWEHeader header =
                    new JWEHeader.Builder(RSA_OAEP_256, A256GCM).keyID(kid).build();
            JWEObject jweObject = new JWEObject(header, new Payload(plaintext));
            jweObject.encrypt(new RSAEncrypter((RSAPublicKey) keyPair.getPublic()));
            return jweObject.serialize();
        }
    }
}
