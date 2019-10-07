package io.jenkins.plugins.casc.misc;

import com.fasterxml.jackson.dataformat.yaml.snakeyaml.Yaml;
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
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.GlobalConfigurationCategory;
import jenkins.tools.ToolConfigurationCategory;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.jvnet.hudson.test.LoggerRule;

import static io.jenkins.plugins.casc.ConfigurationAsCode.serializeYamlNode;
import static io.jenkins.plugins.casc.SchemaGeneration.generateSchema;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Util {

    private static final Logger LOGGER = Logger.getLogger(Util.class.getName());
    private static final String failureMessage  = "The YAML file provided for this schema is invalid";

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
     * @since 1.25
     */
    public static void assertNotInLog(LoggerRule logging, String unexpectedText) {
        assertFalse("The log should not contain '" + unexpectedText + "'",
                logging.getMessages().stream().anyMatch(m -> m.contains(unexpectedText)));
    }

    /**
     * Checks whether {@link LoggerRule} has recorded the message.
     * @param logging Logger rule
     * @param expectedText Text to check
     * @since 1.25
     */
    public static void assertLogContains(LoggerRule logging, String expectedText) {
        assertTrue("The log should contain '" + expectedText + "'",
                logging.getMessages().stream().anyMatch(m -> m.contains(expectedText)));
    }

    /**
     * Converts a given yamlString into a JsonString.
     * Example Usage:
     * <pre>{@code
     * String jsonString = convertToJson(yourYamlString);}
     * </pre>
     * @param yamlString
     * @return JsonString
     */

    public static String convertToJson(String yamlString) {
        Yaml yaml= new Yaml();
        Map<String,Object> map= (Map<String, Object>) yaml.load(yamlString);
        JSONObject jsonObject=new JSONObject(map);
        return jsonObject.toString();
    }

    /**
     * Retrieves the JSON schema for the running jenkins instance.
     * Example Usage:
     *      * <pre>{@code
     *      * Schema jsonSchema = returnSchema();}
     *      * </pre>
     * @return Schema the schema for the current jenkins instance
     */
    public static Schema returnSchema() throws Exception{
        JSONObject schemaObject = generateSchema();
        JSONObject jsonSchema = new JSONObject(
            new JSONTokener(schemaObject.toString()));
        return SchemaLoader.load(jsonSchema);
    }

    /**
     * Validates a given jsonObject against the schema generated for the current live jenkins instance
     *      * Example Usage:
     *      *      * <pre>{@code
     *      *      * assertTrue(validateSchema(jsonSubject));}
     *      *      * </pre>
     * @param  jsonSubject The json Object that needs to be validated
     * @return true if it's valid else returns false
     */
    public static boolean validateSchema(JSONObject jsonSubject) {
        try {
            returnSchema().validate(jsonSubject);
        } catch (Exception ie) {
            LOGGER.log(Level.WARNING, failureMessage, ie);
            return false;
        }
        return true;
    }

    /**
     * Converts a YAML file into a json object
     * Example Usage:
     *           * <pre>{@code
     *           * JSONObject  jsonObject = convertYamlFileToJson("filename");}
     *           * </pre>
     * @param yamlFileName the name of the yaml file that needs to be converted
     * @return JSONObject pertaining to that yaml file.
     */
     public static JSONObject convertYamlFileToJson(Object clazz, String yamlFileName) throws Exception {
        String yamlStringContents = toStringFromYamlFile(clazz, yamlFileName);
        return new JSONObject(new JSONTokener(convertToJson(yamlStringContents)));
    }
}
