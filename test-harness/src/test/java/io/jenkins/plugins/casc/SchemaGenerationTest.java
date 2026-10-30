package io.jenkins.plugins.casc;

import static io.jenkins.plugins.casc.SchemaGeneration.generateSchema;
import static io.jenkins.plugins.casc.misc.Util.convertYamlFileToJson;
import static io.jenkins.plugins.casc.misc.Util.validateSchema;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.JDK;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.DumbSlave;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.NodeProperty;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import java.util.List;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

@WithJenkinsConfiguredWithCode
@SuppressWarnings("unused")
class SchemaGenerationTest {

    @Test
    void validSchemaShouldSucceed(JenkinsConfiguredWithCodeRule j) throws Exception {
        assertThat(validateSchema(convertYamlFileToJson(this, "validSchemaConfig.yml")), empty());
    }

    @Test
    void invalidSchemaShouldNotSucceed(JenkinsConfiguredWithCodeRule j) throws Exception {
        assertThat(
                validateSchema(convertYamlFileToJson(this, "invalidSchemaConfig.yml")),
                contains("#/jenkins/numExecutors: expected type: Integer, found: String"));
    }

    @Test
    void rejectsInvalidBaseConfigurator(JenkinsConfiguredWithCodeRule j) throws Exception {
        assertThat(
                validateSchema(convertYamlFileToJson(this, "invalidBaseConfig.yml")),
                contains("#: extraneous key [invalidBaseConfigurator] is not permitted"));
    }

    @Test
    void validJenkinsBaseConfigurator(JenkinsConfiguredWithCodeRule j) throws Exception {
        assertThat(validateSchema(convertYamlFileToJson(this, "validJenkinsBaseConfig.yml")), empty());
    }

    @Test
    void symbolResolutionForJenkinsBaseConfigurator(JenkinsConfiguredWithCodeRule j) throws Exception {
        assertThat(validateSchema(convertYamlFileToJson(this, "validJenkinsBaseConfigWithSymbol.yml")), empty());
    }

    @Test
    void validSelfConfigurator(JenkinsConfiguredWithCodeRule j) throws Exception {
        assertThat(validateSchema(convertYamlFileToJson(this, "validSelfConfig.yml")), empty());
    }

    @Test
    void attributesNotFlattenedToTopLevel(JenkinsConfiguredWithCodeRule j) throws Exception {
        assertThat(
                validateSchema(convertYamlFileToJson(this, "attributesNotFlattenedToTop.yml")),
                contains("#/tool: extraneous key [acceptLicense] is not permitted"));
    }

    @Test
    void toolInstallationsShouldSucceed(JenkinsConfiguredWithCodeRule j) throws Exception {
        assertThat(validateSchema(convertYamlFileToJson(this, "validToolInstallationsSchemaConfig.yml")), empty());
    }

    @Test
    @ConfiguredWithCode("validToolInstallationsSchemaConfig.yml")
    void toolInstallationsAreWhatConfigurationAccepts(JenkinsConfiguredWithCodeRule j) {
        JDK[] installations =
                j.jenkins.getDescriptorByType(JDK.DescriptorImpl.class).getInstallations();
        assertEquals(1, installations.length);
        assertEquals("jdk17", installations[0].getName());
        assertEquals("/opt/jdk-17", installations[0].getHome());
    }

    @Test
    void rejectsUnknownKeyUnderTool(JenkinsConfiguredWithCodeRule j) throws Exception {
        assertThat(
                validateSchema(convertYamlFileToJson(this, "invalidToolInstallationsSchemaConfig.yml")),
                contains("#/tool/jdk: extraneous key [unknownKey] is not permitted"));
    }

    @Test
    void rejectsObsoleteOrUnknownAttributesInHeteroDescribable(JenkinsConfiguredWithCodeRule j) throws Exception {
        assertThat(
                validateSchema(convertYamlFileToJson(this, "invalidHeteroConfig.yml")),
                contains(
                        "#/jenkins/crumbIssuer/standard: extraneous key [someCompletelyFakeProperty] is not permitted"));
    }

    @Test
    void arrayAttributesShouldGenerateAsArrays(JenkinsConfiguredWithCodeRule j) {
        JSONObject schema = SchemaGeneration.generateSchema();
        JSONObject jenkinsProps =
                schema.getJSONObject("properties").getJSONObject("jenkins").getJSONObject("properties");
        JSONObject agentProtocols = jenkinsProps.getJSONObject("agentProtocols");

        assertNotNull(agentProtocols, "agentProtocols should exist in the generated schema");
        assertEquals("array", agentProtocols.getString("type"), "agentProtocols should be generated as an array type");

        JSONObject items = agentProtocols.getJSONObject("items");
        assertNotNull(items, "agentProtocols should have an 'items' definition");
        assertEquals("string", items.getString("type"), "agentProtocols items should be of type string");
    }

    @Test
    void itemsRootAcceptsItemList(JenkinsConfiguredWithCodeRule j) throws Exception {
        assertThat(validateSchema(convertYamlFileToJson(this, "validItemsSchemaConfig.yml")), empty());
    }

    @Test
    void itemsRootRejectsItemTypeAsKey(JenkinsConfiguredWithCodeRule j) throws Exception {
        assertThat(
                validateSchema(convertYamlFileToJson(this, "invalidItemsRootKeySchemaConfig.yml")),
                contains("#/items: extraneous key [freestyle] is not permitted"));
    }

    @Test
    void itemsRootShouldExposePolymorphicItemSchema(JenkinsConfiguredWithCodeRule j) {
        JSONObject schema = generateSchema();
        JSONObject itemsRoot = schema.getJSONObject("properties").getJSONObject("items");
        JSONObject itemsRootProperties = itemsRoot.getJSONObject("properties");

        assertEquals(
                "string",
                itemsRootProperties.getJSONObject("actionOnUndeclaredItems").getString("type"),
                "actionOnUndeclaredItems should be generated as a string enum");
        assertEquals(
                "array",
                itemsRootProperties.getJSONObject("items").getString("type"),
                "items should be generated as an array");

        JSONObject itemSchema = itemsRootProperties.getJSONObject("items").getJSONObject("items");
        JSONObject itemProperties = itemSchema.getJSONObject("properties");
        assertNotNull(itemProperties.getJSONObject("freestyle"), "freestyle should be present in item schema");
        assertEquals(
                "#/definitions/hudson.model.FreeStyleProject",
                itemProperties.getJSONObject("freestyle").getString("$ref"),
                "freestyle should reference the FreeStyleProject definition");
        assertNotNull(
                itemSchema.getJSONArray("oneOf"), "polymorphic item schema should expose oneOf for item configurators");
        assertNotNull(
                schema.getJSONObject("definitions").getJSONObject("hudson.model.FreeStyleProject"),
                "FreeStyleProject definition should be generated");
    }

    @Test
    void arrayEnumAttributesShouldGenerateAsEnumArrays(JenkinsConfiguredWithCodeRule j) {
        JSONObject schema = generateSchema();

        JSONObject unclassifiedProps =
                schema.getJSONObject("properties").getJSONObject("unclassified").getJSONObject("properties");
        JSONObject dummyConfig = unclassifiedProps.getJSONObject("dummyConfig").getJSONObject("properties");
        JSONObject myEnums = dummyConfig.getJSONObject("myEnums");

        assertNotNull(myEnums, "myEnums should exist in the generated schema");
        assertEquals("array", myEnums.getString("type"), "myEnums should be generated as an array type");

        JSONObject items = myEnums.getJSONObject("items");
        assertNotNull(items, "myEnums should have an 'items' definition");
        assertEquals("string", items.getString("type"), "myEnums items should be of type string");

        JSONArray enumValues = items.getJSONArray("enum");
        assertEquals(2, enumValues.length());
        assertEquals("VALUE_A", enumValues.getString(0));
        assertEquals("VALUE_B", enumValues.getString(1));
    }

    public enum DummyEnum {
        VALUE_A,
        VALUE_B
    }

    @Extension
    @Symbol("dummyConfig")
    public static class DummyConfig extends GlobalConfiguration {
        private List<DummyEnum> myEnums;

        @DataBoundConstructor
        public DummyConfig() {}

        public List<DummyEnum> getMyEnums() {
            return myEnums;
        }

        @DataBoundSetter
        public void setMyEnums(List<DummyEnum> myEnums) {
            this.myEnums = myEnums;
        }
    }

    @Test
    void validArraySchemaShouldSucceed(JenkinsConfiguredWithCodeRule j) throws Exception {
        assertThat(validateSchema(convertYamlFileToJson(this, "validArraySchemaConfig.yml")), empty());
    }

    @Test
    void listItemAttributesKeepTheirTypes(JenkinsConfiguredWithCodeRule j) throws Exception {
        assertThat(validateSchema(convertYamlFileToJson(this, "validListItemSchemaConfig.yml")), empty());
    }

    @Test
    void listItemEnumRejectsUnknownConstant(JenkinsConfiguredWithCodeRule j) throws Exception {
        assertThat(
                validateSchema(convertYamlFileToJson(this, "invalidListItemSchemaConfig.yml")),
                contains("#/unclassified/listItemConfig/entries/0/mode: VALUE_C is not a valid enum value"));
    }

    public static class ListItem {
        private final DummyEnum mode;
        private List<String> names;

        @DataBoundConstructor
        public ListItem(DummyEnum mode) {
            this.mode = mode;
        }

        public DummyEnum getMode() {
            return mode;
        }

        public List<String> getNames() {
            return names;
        }

        @DataBoundSetter
        public void setNames(List<String> names) {
            this.names = names;
        }
    }

    @Extension
    @Symbol("listItemConfig")
    public static class ListItemConfig extends GlobalConfiguration {
        private List<ListItem> entries;

        @DataBoundConstructor
        public ListItemConfig() {}

        public List<ListItem> getEntries() {
            return entries;
        }

        @DataBoundSetter
        public void setEntries(List<ListItem> entries) {
            this.entries = entries;
        }
    }

    @Test
    void globalNodePropertiesIsAListOfNodeProperties(JenkinsConfiguredWithCodeRule j) throws Exception {
        assertThat(validateSchema(convertYamlFileToJson(this, "validGlobalNodePropertiesSchemaConfig.yml")), empty());
    }

    @Test
    @ConfiguredWithCode("validGlobalNodePropertiesSchemaConfig.yml")
    void globalNodePropertiesSchemaMatchesWhatConfigurationAccepts(JenkinsConfiguredWithCodeRule j) {
        EnvironmentVariablesNodeProperty envVars =
                j.jenkins.getGlobalNodeProperties().get(EnvironmentVariablesNodeProperty.class);
        assertNotNull(envVars, "envVars node property should be configured");
        assertEquals("bar", envVars.getEnvVars().get("FOO"));
    }

    @Test
    void jenkinsRootAcceptsNodeProperties(JenkinsConfiguredWithCodeRule j) throws Exception {
        assertThat(validateSchema(convertYamlFileToJson(this, "validNodePropertiesSchemaConfig.yml")), empty());
    }

    @Test
    void jenkinsRootAcceptsNodes(JenkinsConfiguredWithCodeRule j) throws Exception {
        assertThat(validateSchema(convertYamlFileToJson(this, "validNodesSchemaConfig.yml")), empty());
    }

    @Test
    @ConfiguredWithCode("validNodesSchemaConfig.yml")
    void jenkinsRootNodesAreWhatConfigurationAccepts(JenkinsConfiguredWithCodeRule j) {
        Node agent = j.jenkins.getNode("agent1");
        assertInstanceOf(DumbSlave.class, agent);
        assertEquals("/home/jenkins", ((DumbSlave) agent).getRemoteFS());
    }

    @Test
    void jenkinsRootRejectsNestedConfigurators(JenkinsConfiguredWithCodeRule j) throws Exception {
        assertThat(
                validateSchema(convertYamlFileToJson(this, "invalidJenkinsRootKeySchemaConfig.yml")),
                contains("#/jenkins: extraneous key [permanent] is not permitted"));
    }

    @Test
    void globalNodePropertiesRejectsUnknownPropertyType(JenkinsConfiguredWithCodeRule j) throws Exception {
        assertThat(
                validateSchema(convertYamlFileToJson(this, "invalidGlobalNodePropertiesSchemaConfig.yml")),
                hasItem("#/jenkins/globalNodeProperties/0: extraneous key [unknownNodeProperty] is not permitted"));
    }

    @Test
    void enumsAreListedByConstantName(JenkinsConfiguredWithCodeRule j) throws Exception {
        assertThat(validateSchema(convertYamlFileToJson(this, "validEnumNameSchemaConfig.yml")), empty());
    }

    @Test
    @ConfiguredWithCode("validEnumNameSchemaConfig.yml")
    void enumConstantNamesAreWhatConfigurationAccepts(JenkinsConfiguredWithCodeRule j) {
        EnumNameConfig config = GlobalConfiguration.all().get(EnumNameConfig.class);
        assertEquals(RenamedEnum.LOWER_CASED, config.getSpelling());
        assertEquals(List.of(RenamedEnum.LOWER_CASED), config.getSpellings());
    }

    public enum RenamedEnum {
        LOWER_CASED;

        @Override
        public String toString() {
            return "lower-cased";
        }
    }

    @Extension
    @Symbol("enumNameConfig")
    public static class EnumNameConfig extends GlobalConfiguration {
        private RenamedEnum spelling;
        private List<RenamedEnum> spellings;

        @DataBoundConstructor
        public EnumNameConfig() {}

        public RenamedEnum getSpelling() {
            return spelling;
        }

        @DataBoundSetter
        public void setSpelling(RenamedEnum spelling) {
            this.spelling = spelling;
        }

        public List<RenamedEnum> getSpellings() {
            return spellings;
        }

        @DataBoundSetter
        public void setSpellings(List<RenamedEnum> spellings) {
            this.spellings = spellings;
        }
    }

    @Test
    void heteroDescribableAcceptsEverySymbol(JenkinsConfiguredWithCodeRule j) throws Exception {
        assertThat(validateSchema(convertYamlFileToJson(this, "validHeteroSymbolsSchemaConfig.yml")), empty());
    }

    @Test
    @ConfiguredWithCode("validHeteroSymbolsSchemaConfig.yml")
    void heteroDescribableSymbolsAreWhatConfigurationAccepts(JenkinsConfiguredWithCodeRule j) {
        ShapeConfig config = GlobalConfiguration.all().get(ShapeConfig.class);
        assertInstanceOf(RingShape.class, config.getPreferred());
        assertInstanceOf(RingShape.class, config.getAlias());
        assertInstanceOf(RingShape.class, config.getDerived());
    }

    @Test
    void heteroDescribableAliasesAreDeprecated(JenkinsConfiguredWithCodeRule j) {
        JSONObject shapeProperties = generateSchema()
                .getJSONObject("definitions")
                .getJSONObject(Shape.class.getName())
                .getJSONObject("properties");

        assertFalse(shapeProperties.getJSONObject("round").has("deprecated"), "preferred symbol is not deprecated");
        assertTrue(shapeProperties.getJSONObject("circle").getBoolean("deprecated"), "@Symbol alias is deprecated");
        assertTrue(shapeProperties.getJSONObject("ring").getBoolean("deprecated"), "derived name is deprecated");
    }

    @Test
    void heteroDescribableRejectsUnknownSymbol(JenkinsConfiguredWithCodeRule j) throws Exception {
        assertThat(
                validateSchema(convertYamlFileToJson(this, "invalidHeteroSymbolsSchemaConfig.yml")),
                hasItem("#/unclassified/shapeConfig/preferred: extraneous key [square] is not permitted"));
    }

    @Test
    void heteroListItemsAcceptEverySymbol(JenkinsConfiguredWithCodeRule j) throws Exception {
        assertThat(validateSchema(convertYamlFileToJson(this, "validHeteroListSymbolsSchemaConfig.yml")), empty());
    }

    @Test
    void heteroListItemAliasesAreDeprecated(JenkinsConfiguredWithCodeRule j) {
        JSONObject itemProperties = generateSchema()
                .getJSONObject("properties")
                .getJSONObject("unclassified")
                .getJSONObject("properties")
                .getJSONObject("shapeConfig")
                .getJSONObject("properties")
                .getJSONObject("shapes")
                .getJSONObject("items")
                .getJSONObject("properties");

        assertFalse(itemProperties.getJSONObject("round").has("deprecated"), "preferred symbol is not deprecated");
        assertTrue(itemProperties.getJSONObject("circle").getBoolean("deprecated"), "@Symbol alias is deprecated");
        assertTrue(itemProperties.getJSONObject("ring").getBoolean("deprecated"), "derived name is deprecated");
    }

    @Test
    void heteroListItemsRejectUnknownSymbol(JenkinsConfiguredWithCodeRule j) throws Exception {
        List<String> errors = validateSchema(convertYamlFileToJson(this, "invalidHeteroListSymbolsSchemaConfig.yml"));
        assertThat(errors, hasItem("#/unclassified/shapeConfig/shapes/0: extraneous key [square] is not permitted"));
        // the abstract type itself is not something configuration can create
        assertThat(errors, hasItem("#/unclassified/shapeConfig/shapes/1: extraneous key [shape] is not permitted"));
    }

    @Test
    void heteroDescribableAcceptsShortForm(JenkinsConfiguredWithCodeRule j) throws Exception {
        assertThat(validateSchema(convertYamlFileToJson(this, "validHeteroShortFormSchemaConfig.yml")), empty());
    }

    @Test
    @ConfiguredWithCode("validHeteroShortFormSchemaConfig.yml")
    void heteroDescribableShortFormIsWhatConfigurationAccepts(JenkinsConfiguredWithCodeRule j) {
        ShapeConfig config = GlobalConfiguration.all().get(ShapeConfig.class);
        assertInstanceOf(RingShape.class, config.getPreferred());
        assertInstanceOf(RingShape.class, config.getAlias());
        assertInstanceOf(RingShape.class, config.getDerived());
    }

    @Test
    void heteroDescribableFieldOnlyReferencesItsDefinition(JenkinsConfiguredWithCodeRule j) {
        JSONObject shapeConfigProperties = generateSchema()
                .getJSONObject("properties")
                .getJSONObject("unclassified")
                .getJSONObject("properties")
                .getJSONObject("shapeConfig")
                .getJSONObject("properties");

        // a "type": "object" sibling would reject the short form wherever $ref siblings are applied
        JSONObject hetero = shapeConfigProperties.getJSONObject("preferred");
        assertEquals("#/definitions/" + Shape.class.getName(), hetero.getString("$ref"));
        assertFalse(hetero.has("type"), "heterogeneous describable field has no type next to its $ref");
        assertFalse(hetero.has("additionalProperties"), "heterogeneous describable field has no additionalProperties");

        JSONObject concrete = shapeConfigProperties.getJSONObject("concrete");
        assertEquals("#/definitions/" + RingShape.class.getName(), concrete.getString("$ref"));
        assertEquals("object", concrete.getString("type"));
        assertFalse(concrete.getBoolean("additionalProperties"));
    }

    @Test
    void heteroListItemsAcceptShortForm(JenkinsConfiguredWithCodeRule j) throws Exception {
        assertThat(validateSchema(convertYamlFileToJson(this, "validHeteroListShortFormSchemaConfig.yml")), empty());
    }

    @Test
    void heteroShortFormRejectsUnknownSymbol(JenkinsConfiguredWithCodeRule j) throws Exception {
        List<String> errors = validateSchema(convertYamlFileToJson(this, "invalidHeteroShortFormSchemaConfig.yml"));
        assertThat(errors, hasItem(containsString("#/unclassified/shapeConfig/preferred")));
        assertThat(errors, hasItem(containsString("#/unclassified/shapeConfig/shapes/1")));
        assertThat(errors, not(hasItem(containsString("#/unclassified/shapeConfig/shapes/0"))));
    }

    @Test
    @ConfiguredWithCode("validNodePropertiesSchemaConfig.yml")
    void heteroListItemSymbolsAreWhatConfigurationAccepts(JenkinsConfiguredWithCodeRule j) throws Exception {
        EnvVars env = new EnvVars();
        for (NodeProperty<?> property : Jenkins.get().getNodeProperties()) {
            property.buildEnvVars(env, TaskListener.NULL);
        }
        assertEquals("bar", env.get("FOO"));
    }

    public abstract static class Shape implements Describable<Shape> {}

    public static class RingShape extends Shape {
        @DataBoundConstructor
        public RingShape() {}

        @Extension
        @Symbol({"round", "circle"})
        public static class DescriptorImpl extends Descriptor<Shape> {}
    }

    @Extension
    @Symbol("shapeConfig")
    public static class ShapeConfig extends GlobalConfiguration {
        private Shape preferred;
        private Shape alias;
        private Shape derived;
        private List<Shape> shapes;
        private RingShape concrete;

        @DataBoundConstructor
        public ShapeConfig() {}

        public RingShape getConcrete() {
            return concrete;
        }

        @DataBoundSetter
        public void setConcrete(RingShape concrete) {
            this.concrete = concrete;
        }

        public Shape getPreferred() {
            return preferred;
        }

        @DataBoundSetter
        public void setPreferred(Shape preferred) {
            this.preferred = preferred;
        }

        public Shape getAlias() {
            return alias;
        }

        @DataBoundSetter
        public void setAlias(Shape alias) {
            this.alias = alias;
        }

        public Shape getDerived() {
            return derived;
        }

        @DataBoundSetter
        public void setDerived(Shape derived) {
            this.derived = derived;
        }

        public List<Shape> getShapes() {
            return shapes;
        }

        @DataBoundSetter
        public void setShapes(List<Shape> shapes) {
            this.shapes = shapes;
        }
    }

    //    For testing manually
    //    @Test
    //    public void writeSchema() throws Exception {
    //        BufferedWriter writer = new BufferedWriter(new FileWriter("schema.json"));
    //        writer.write(writeJSONSchema());
    //        writer.close();
    //    }
}
