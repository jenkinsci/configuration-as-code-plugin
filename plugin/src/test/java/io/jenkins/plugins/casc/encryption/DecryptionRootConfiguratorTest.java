package io.jenkins.plugins.casc.encryption;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jenkins.plugins.casc.ConfigurationAsCode;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.RootElementConfigurator;
import io.jenkins.plugins.casc.UnknownAttributesException;
import io.jenkins.plugins.casc.encryption.DecryptionRootConfigurator.DecryptionConfiguration;
import io.jenkins.plugins.casc.model.Mapping;
import io.jenkins.plugins.casc.model.Scalar;
import io.jenkins.plugins.casc.model.Source;
import io.jenkins.plugins.casc.yaml.YamlSource;
import io.jenkins.plugins.casc.yaml.YamlUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class DecryptionRootConfiguratorTest {

    private final DecryptionRootConfigurator configurator = new DecryptionRootConfigurator();

    @Test
    void validYamlConfigurationPopulatesContextAcrossCheckConfigureAndInitialize(JenkinsRule j) throws Exception {
        String nonexistentButValidPath = "/nonexistent/casc-keys/private key.pem";
        String yaml = """
                decryption:
                  jwe:
                    keyFile: "  %s  "
                """.formatted(nonexistentButValidPath);

        ConfigurationContext checkContext = newContext();
        Mapping checkMapping = loadYaml(yaml, checkContext);
        configurator.check(checkMapping.get("decryption"), checkContext);
        assertThat(checkContext.jweKeyFileFromConfiguration(), equalTo(nonexistentButValidPath));

        ConfigurationContext configureContext = newContext();
        Mapping configureMapping = loadYaml(yaml, configureContext);
        configurator.configure(configureMapping.get("decryption"), configureContext);
        assertThat(configureContext.jweKeyFileFromConfiguration(), equalTo(nonexistentButValidPath));

        ConfigurationContext initContext = newContext();
        Mapping initMapping = loadYaml(yaml, initContext);
        DecryptionRootConfigurator.initialize(initMapping, initContext);
        assertThat(initContext.jweKeyFileFromConfiguration(), equalTo(nonexistentButValidPath));
        assertTrue(initMapping.containsKey("decryption"), "initialize must not remove root section prematurely");
        assertTrue(
                initMapping.get("decryption").asMapping().containsKey("jwe"),
                "initialize must not strip nested jwe entry before normal configurator processing");
    }

    @Test
    void missingDecryptionSectionDoesNotFailAndLeavesContextUnset(JenkinsRule j) throws Exception {
        ConfigurationContext context = newContext();
        Mapping entries = loadYaml("jenkins:\n  systemMessage: \"hello\"\n", context);

        DecryptionRootConfigurator.initialize(entries, context);

        assertThat(context.jweKeyFileFromConfiguration(), is(nullValue()));
        assertThat(ConfigurationAsCode.get().checkWith(entries, context).entrySet(), is(empty()));
    }

    @Test
    void decryptionRootIsRegisteredAndConsumedWithoutUnknownRootError(JenkinsRule j) throws Exception {
        boolean registered = RootElementConfigurator.all().stream()
                .anyMatch(c -> c instanceof DecryptionRootConfigurator && "decryption".equals(c.getName()));
        assertTrue(registered, "DecryptionRootConfigurator must be registered with name 'decryption'");

        ConfigurationContext context = newContext();
        String yaml = """
                decryption:
                  jwe:
                    keyFile: /path/to/private-key.pem
                jenkins:
                  systemMessage: "Configured cleanly"
                """;
        Mapping entries = loadYaml(yaml, context);

        Map<Source, String> issues = ConfigurationAsCode.get().checkWith(entries, context);

        assertThat(issues.entrySet(), is(empty()));
        assertThat(context.jweKeyFileFromConfiguration(), equalTo("/path/to/private-key.pem"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"decryption:\n", "decryption: {}\n"})
    void missingJweSectionIsRejected(String yaml, JenkinsRule j) throws Exception {
        ConfigurationContext context = newContext();
        Mapping entries = loadYaml(yaml, context);

        ConfiguratorException ex = assertThrows(
                ConfiguratorException.class, () -> DecryptionRootConfigurator.initialize(entries, context));

        assertThat(ex.getMessage(), containsString("Missing required configuration: 'decryption.jwe.keyFile'"));
        assertThat(context.jweKeyFileFromConfiguration(), is(nullValue()));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "decryption:\n  jwe:\n",
                "decryption:\n  jwe: {}\n",
                "decryption:\n  jwe:\n    keyFile: \"\"\n",
                "decryption:\n  jwe:\n    keyFile: \"   \"\n"
            })
    void missingEmptyOrWhitespaceKeyFileIsRejected(String yaml, JenkinsRule j) throws Exception {
        ConfigurationContext context = newContext();
        Mapping entries = loadYaml(yaml, context);

        assertThrows(ConfiguratorException.class, () -> DecryptionRootConfigurator.initialize(entries, context));
        assertThat(context.jweKeyFileFromConfiguration(), is(nullValue()));
    }

    @Test
    void duplicateCaseInsensitiveAndMalformedDecryptionSectionsAreRejected(JenkinsRule j) throws Exception {
        ConfigurationContext context = newContext();
        Mapping duplicateEntries = new Mapping();
        duplicateEntries.put("decryption", Mapping.EMPTY);
        duplicateEntries.put("DECRYPTION", Mapping.EMPTY);

        ConfiguratorException duplicateEx = assertThrows(
                ConfiguratorException.class, () -> DecryptionRootConfigurator.initialize(duplicateEntries, context));
        assertThat(duplicateEx.getMessage(), containsString("Duplicate 'decryption' configuration section."));

        Mapping scalarEntries = new Mapping();
        scalarEntries.put("decryption", new Scalar("/path/to/key.pem"));
        assertThrows(ConfiguratorException.class, () -> DecryptionRootConfigurator.initialize(scalarEntries, context));
    }

    @Test
    void unknownPropertiesFollowConfiguredUnknownAttributePolicy(JenkinsRule j) throws Exception {
        String yamlWithUnknownNested = """
                decryption:
                  jwe:
                    keyFile: /path/to/key.pem
                    unexpectedNested: ignored-or-rejected
                """;
        String yamlWithUnknownTopLevel = """
                decryption:
                  unexpectedTop: ignored-or-rejected
                  jwe:
                    keyFile: /path/to/key.pem
                """;

        ConfigurationContext rejectContext = newContext();
        Mapping rejectNested = loadYaml(yamlWithUnknownNested, rejectContext);
        assertThrows(
                UnknownAttributesException.class,
                () -> DecryptionRootConfigurator.initialize(rejectNested, rejectContext));

        Mapping rejectTop = loadYaml(yamlWithUnknownTopLevel, rejectContext);
        assertThrows(
                UnknownAttributesException.class,
                () -> DecryptionRootConfigurator.initialize(rejectTop, rejectContext));

        ConfigurationContext warnContext = newContext();
        warnContext.setUnknown(ConfigurationContext.Unknown.warn);
        Mapping warnNested = loadYaml(yamlWithUnknownNested, warnContext);

        Map<Source, String> issues = ConfigurationAsCode.get().checkWith(warnNested, warnContext);

        assertThat(warnContext.jweKeyFileFromConfiguration(), equalTo("/path/to/key.pem"));
        assertTrue(
                issues.values().stream().anyMatch(msg -> msg.contains("unexpectedNested")),
                "checkWith should record warning for unknown nested property under Unknown.warn policy");
    }

    @Test
    void describeReturnsNullAndExportDoesNotLeakKeyPath(JenkinsRule j) throws Exception {
        ConfigurationContext context = newContext();
        DecryptionConfiguration target = configurator.getTargetComponent(context);
        assertThat(configurator.describe(target, context), is(nullValue()));

        String yaml = """
                decryption:
                  jwe:
                    keyFile: /secret/private-key-do-not-export.pem
                jenkins:
                  systemMessage: "Export check"
                """;
        ConfigurationAsCode.get().configureWith(YamlSource.of(new ByteArrayInputStream(yaml.getBytes(UTF_8))));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ConfigurationAsCode.get().export(out);
        String exported = out.toString(UTF_8);

        assertThat(exported, containsString("Export check"));
        assertThat(exported, not(containsString("decryption:")));
        assertThat(exported, not(containsString("/secret/private-key-do-not-export.pem")));
    }

    private static ConfigurationContext newContext() {
        return new ConfigurationContext(ConfiguratorRegistry.get());
    }

    private static Mapping loadYaml(String yaml, ConfigurationContext context) throws ConfiguratorException {
        return YamlUtils.loadFrom(List.of(YamlSource.of(new ByteArrayInputStream(yaml.getBytes(UTF_8)))), context);
    }
}
