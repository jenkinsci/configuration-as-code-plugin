package io.jenkins.plugins.casc;

import static io.jenkins.plugins.casc.SchemaGeneration.generateSchema;
import static io.jenkins.plugins.casc.misc.Util.convertYamlFileToJson;
import static io.jenkins.plugins.casc.misc.Util.validateSchema;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import hudson.Extension;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import java.util.List;
import jenkins.model.GlobalConfiguration;
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

    //    For testing manually
    //    @Test
    //    public void writeSchema() throws Exception {
    //        BufferedWriter writer = new BufferedWriter(new FileWriter("schema.json"));
    //        writer.write(writeJSONSchema());
    //        writer.close();
    //    }
}
