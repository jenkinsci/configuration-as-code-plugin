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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import jenkins.model.GlobalConfigurationCategory;
import jenkins.model.Jenkins;

import static io.jenkins.plugins.casc.ConfigurationAsCode.serializeYamlNode;

public class Util {

    /**
     * Gets the jenkins configurator
     * @return the jenkins configurator
     */
    public static JenkinsConfigurator getJenkinsConfigurator() {
        return Jenkins.getInstance().getExtensionList(JenkinsConfigurator.class).get(0);
    }

    /**
     * Gets the JenkinsRoot Mapping <br/>
     *
     * Example usage:
     * <pre>{@code
     * ConfiguratorRegistry registry = ConfiguratorRegistry.get();
     * ConfigurationContext context = new ConfigurationContext(registry);
     * final CNode configNode = getJenkinsRoot(context).get("nodes");
     * }</pre>
     *
     * @param context the configuration context
     * @return the jenkins root mapping
     * @throws Exception something's not right...
     */
    public static Mapping getJenkinsRoot(ConfigurationContext context)
        throws Exception {
        JenkinsConfigurator root = getJenkinsConfigurator();
        return Objects.requireNonNull(root.describe(root.getTargetComponent(context), context)).asMapping();
    }

    /**
     * Gets the Unclassified root Mapping <br/>
     *
     * Example usage:
     * <pre>{@code
     * ConfiguratorRegistry registry = ConfiguratorRegistry.get();
     * ConfigurationContext context = new ConfigurationContext(registry);
     * final CNode configNode = getUnclassifiedRoot(context).get("my-plugin-attribute");
     * }</pre>
     *
     * @param context the configuration context
     * @return the unclassified root mapping
     * @throws Exception something's not right...
     */
    public static Mapping getUnclassifiedRoot(ConfigurationContext context)
            throws Exception {
        GlobalConfigurationCategory.Unclassified unclassified = ExtensionList.lookup(GlobalConfigurationCategory.Unclassified.class).get(0);

        GlobalConfigurationCategoryConfigurator unclassifiedConfigurator = new GlobalConfigurationCategoryConfigurator(unclassified);
        return Objects.requireNonNull(unclassifiedConfigurator.describe(unclassifiedConfigurator.getTargetComponent(context), context)).asMapping();
    }

    /**
     * Gets the SecurityRoot Mapping <br/>
     *
     * Example usage:
     * <pre>{@code
     * ConfiguratorRegistry registry = ConfiguratorRegistry.get();
     * ConfigurationContext context = new ConfigurationContext(registry);
     * final CNode configNode = getSecurityRoot(context).get("GlobalJobDslSecurityConfiguration");
     * }</pre>
     *
     * @param context the configuration context
     * @return the security root mapping
     * @throws Exception something's not right...
     */
    public static Mapping getSecurityRoot(ConfigurationContext context)
            throws Exception {
        GlobalConfigurationCategory.Security security = ExtensionList.lookup(GlobalConfigurationCategory.Security.class).get(0);

        GlobalConfigurationCategoryConfigurator securityConfigurator = new GlobalConfigurationCategoryConfigurator(security);
        return Objects.requireNonNull(securityConfigurator.describe(securityConfigurator.getTargetComponent(context), context)).asMapping();
    }

    /**
     * Converts a given CNode into a string <br/>
     *
     * Example usage:
     * <pre>{@code
     * ConfiguratorRegistry registry = ConfiguratorRegistry.get();
     * ConfigurationContext context = new ConfigurationContext(registry);
     * final CNode yourAttribute = getUnclassifiedRoot(context).get("<your-attribute>");
     *
     * String exported = toYamlString(yourAttribute);
     * }</pre>
     *
     * @param rootNode the CNode to convert to a string
     * @return a yaml string
     * @throws IOException if exporting to yaml fails
     */
    public static String toYamlString(CNode rootNode) throws IOException {
        Node yamlRoot = ConfigurationAsCode.get().toYaml(rootNode);
        StringWriter buffer = new StringWriter();
        serializeYamlNode(yamlRoot, buffer);
        return buffer.toString();
    }

    /**
     * Reads a resource from the classpath to use in asserting expected export content
     *
     * <pre>Example usage:
     *  {@code toStringFromYamlFile(this, "expectedOutput.yaml");}</pre>
     *
     * @param clazz pass in `this`
     * @param resourcePath the file name to read, should be in the same package as your test class in resources
     * @return the string content of the file
     * @throws URISyntaxException invalid path
     * @throws IOException invalid path or file not found in general
     */
    public static String toStringFromYamlFile(Object clazz, String resourcePath) throws URISyntaxException, IOException {
        URL resource = clazz.getClass()
                .getResource(resourcePath);
        if (resource == null) {
            throw new FileNotFoundException("Couldn't find file: " + resourcePath);
        }

        return new String(Files.readAllBytes(Paths.get(resource.toURI())));
    }
}
