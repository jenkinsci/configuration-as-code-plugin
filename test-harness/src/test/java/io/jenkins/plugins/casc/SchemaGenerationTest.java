package io.jenkins.plugins.casc;

import static io.jenkins.plugins.casc.misc.Util.convertYamlFileToJson;
import static io.jenkins.plugins.casc.misc.Util.validateSchema;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;

import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import org.junit.jupiter.api.Test;

@WithJenkinsConfiguredWithCode
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

    //    For testing manually
    //    @Test
    //    public void writeSchema() throws Exception {
    //        BufferedWriter writer = new BufferedWriter(new FileWriter("schema.json"));
    //        writer.write(writeJSONSchema());
    //        writer.close();
    //    }
}
