package io.jenkins.plugins.casc;

import static io.jenkins.plugins.casc.misc.Util.convertYamlFileToJson;
import static io.jenkins.plugins.casc.misc.Util.validateSchema;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import hudson.Extension;
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
    void arrayEnumAttributesShouldGenerateAsEnumArrays(JenkinsConfiguredWithCodeRule j) {
        JSONObject schema = SchemaGeneration.generateSchema();

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

    //    For testing manually
    //    @Test
    //    public void writeSchema() throws Exception {
    //        BufferedWriter writer = new BufferedWriter(new FileWriter("schema.json"));
    //        writer.write(writeJSONSchema());
    //        writer.close();
    //    }
}
