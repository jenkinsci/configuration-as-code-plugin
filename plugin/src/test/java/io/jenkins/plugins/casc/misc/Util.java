package io.jenkins.plugins.casc.misc;

import hudson.ExtensionList;
import io.jenkins.plugins.casc.ConfigurationAsCode;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.core.JenkinsConfigurator;
import io.jenkins.plugins.casc.impl.configurators.GlobalConfigurationCategoryConfigurator;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import io.jenkins.plugins.casc.snakeyaml.nodes.Node;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import jenkins.model.GlobalConfigurationCategory;
import jenkins.tools.ToolConfigurationCategory;
import org.jvnet.hudson.test.LoggerRule;

import static io.jenkins.plugins.casc.ConfigurationAsCode.serializeYamlNode;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Util {

    /**
     * Gets the Jenkins configurator.
     *
     * @return the Jenkins configurator
     */
    public static JenkinsConfigurator getJenkinsConfigurator() {
        return ExtensionList.lookup(JenkinsConfigurator.class).get(0);
    }

    /**
     * Gets the "jenkins" root mapping.
     * <p>
     * Example usage:
     * <pre>{@code
     * ConfiguratorRegistry registry = ConfiguratorRegistry.get();
     * ConfigurationContext context = new ConfigurationContext(registry);
     * CNode configNode = getJenkinsRoot(context).get("nodes");}</pre>
     *
     * @param context the configuration context
     * @return the "jenkins" root mapping
     * @throws Exception something's not right...
     */
    public static Mapping getJenkinsRoot(ConfigurationContext context) throws Exception {
        JenkinsConfigurator root = getJenkinsConfigurator();

        return Objects.requireNonNull(root.describe(root.getTargetComponent(context), context)).asMapping();
    }

    /**
     * Gets the "unclassified" root mapping.
     * <p>
     * Example usage:
     * <pre>{@code
     * ConfiguratorRegistry registry = ConfiguratorRegistry.get();
     * ConfigurationContext context = new ConfigurationContext(registry);
     * CNode configNode = getUnclassifiedRoot(context).get("my-plugin-attribute");}</pre>
     *
     * @param context the configuration context
     * @return the "unclassified" root mapping
     * @throws Exception something's not right...
     */
    public static Mapping getUnclassifiedRoot(ConfigurationContext context) throws Exception {
        GlobalConfigurationCategory.Unclassified unclassified = ExtensionList.lookup(GlobalConfigurationCategory.Unclassified.class).get(0);

        GlobalConfigurationCategoryConfigurator unclassifiedConfigurator = new GlobalConfigurationCategoryConfigurator(unclassified);
        return Objects.requireNonNull(unclassifiedConfigurator.describe(unclassifiedConfigurator.getTargetComponent(context), context)).asMapping();
    }

    /**
     * Gets the "security" root mapping.
     * <p>
     * Example usage:
     * <pre>{@code
     * ConfiguratorRegistry registry = ConfiguratorRegistry.get();
     * ConfigurationContext context = new ConfigurationContext(registry);
     * CNode configNode = getSecurityRoot(context).get("GlobalJobDslSecurityConfiguration");}</pre>
     *
     * @param context the configuration context
     * @return the "security" root mapping
     * @throws Exception something's not right...
     */
    public static Mapping getSecurityRoot(ConfigurationContext context) throws Exception {
        GlobalConfigurationCategory.Security security = ExtensionList.lookup(GlobalConfigurationCategory.Security.class).get(0);

        GlobalConfigurationCategoryConfigurator securityConfigurator = new GlobalConfigurationCategoryConfigurator(security);
        return Objects.requireNonNull(securityConfigurator.describe(securityConfigurator.getTargetComponent(context), context)).asMapping();
    }

    /**
     * Gets the "tool" root mapping.
     * <p>
     * Example usage:
     * <pre>{@code
     * ConfiguratorRegistry registry = ConfiguratorRegistry.get();
     * ConfigurationContext context = new ConfigurationContext(registry);
     * CNode configNode = getToolRoot(context).get("git");}</pre>
     *
     * @param context the configuration context
     * @return the "tool" root mapping
     * @throws Exception something's not right...
     */
    public static Mapping getToolRoot(ConfigurationContext context) throws Exception {
        ToolConfigurationCategory category = ExtensionList.lookup(ToolConfigurationCategory.class).get(0);

        GlobalConfigurationCategoryConfigurator configurator = new GlobalConfigurationCategoryConfigurator(category);
        return Objects.requireNonNull(configurator.describe(configurator.getTargetComponent(context), context)).asMapping();
    }

    /**
     * Converts a given {@code CNode} into a string.
     * <p>
     * Example usage:
     * <pre>{@code
     * ConfiguratorRegistry registry = ConfiguratorRegistry.get();
     * ConfigurationContext context = new ConfigurationContext(registry);
     * CNode yourAttribute = getUnclassifiedRoot(context).get("<your-attribute>");
     *
     * String exported = toYamlString(yourAttribute);}</pre>
     *
     * @param rootNode the {@code CNode} to convert to a string
     * @return a YAML string
     * @throws IOException if exporting to YAML fails
     */
    public static String toYamlString(CNode rootNode) throws IOException {
        Node yamlRoot = ConfigurationAsCode.get().toYaml(rootNode);
        StringWriter buffer = new StringWriter();
        serializeYamlNode(yamlRoot, buffer);
        return buffer.toString();
    }

    /**
     * Reads a resource from the classpath to use in asserting expected export content.
     * <p>
     * The resource is expected to be UTF-8 encoded.
     * <p>
     * Example usage:
     * <pre>{@code
     * toStringFromYamlFile(this, "expected-output.yml");}</pre>
     *
     * @param clazz pass in {@code this}
     * @param resourcePath the file name to read, should be in the same package as your test class in resources
     * @return the string content of the file
     * @throws URISyntaxException invalid path
     * @throws IOException invalid path or file not found in general
     */
    public static String toStringFromYamlFile(Object clazz, String resourcePath) throws URISyntaxException, IOException {
        URL resource = clazz.getClass().getResource(resourcePath);
        if (resource == null) {
            throw new FileNotFoundException("Couldn't find file: " + resourcePath);
        }

        byte[] bytes = Files.readAllBytes(Paths.get(resource.toURI()));
        return new String(bytes, StandardCharsets.UTF_8).replaceAll("\r\n?", "\n");
    }

    /**
     * Checks whether {@link LoggerRule} has not recorded the message.
     * @param logging Logger rule
     * @param unexpectedText Text to check
     * @since TODO
     */
    public static void assertNotInLog(LoggerRule logging, String unexpectedText) {
        assertFalse("The log should not contain '" + unexpectedText + "'",
                logging.getMessages().stream().anyMatch(m -> m.contains(unexpectedText)));
    }

    /**
     * Checks whether {@link LoggerRule} has recorded the message.
     * @param logging Logger rule
     * @param expectedText Text to check
     * @since TODO
     */
    public static void assertLogContains(LoggerRule logging, String expectedText) {
        assertTrue("The log should contain '" + expectedText + "'",
                logging.getMessages().stream().anyMatch(m -> m.contains(expectedText)));
    }
}
