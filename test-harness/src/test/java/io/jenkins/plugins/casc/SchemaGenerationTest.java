package io.jenkins.plugins.casc;

import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.Rule;
import org.junit.Test;

import static io.jenkins.plugins.casc.misc.Util.convertYamlFileToJson;
import static io.jenkins.plugins.casc.misc.Util.validateSchema;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;


public class SchemaGenerationTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    public void validSchemaShouldSucceed() throws Exception {
        assertThat(validateSchema(convertYamlFileToJson(this, "validSchemaConfig.yml")), empty());
    }

    @Test
    public void invalidSchemaShouldNotSucceed() throws Exception {
        assertThat(validateSchema(convertYamlFileToJson(this, "invalidSchemaConfig.yml")),
            contains("#/jenkins/numExecutors: expected type: Number, found: String"));
    }

    @Test
    public void rejectsInvalidBaseConfigurator() throws Exception {
        assertThat(validateSchema(convertYamlFileToJson(this, "invalidBaseConfig.yml")),
            contains("#: extraneous key [invalidBaseConfigurator] is not permitted"));
    }

    @Test
    public void validJenkinsBaseConfigurator() throws Exception {
        assertThat(validateSchema(convertYamlFileToJson(this, "validJenkinsBaseConfig.yml")),
            empty());
    }

    @Test
    public void symbolResolutionForJenkinsBaseConfigurator() throws Exception {
        assertThat(validateSchema(convertYamlFileToJson(this, "validJenkinsBaseConfigWithSymbol.yml")),
            empty());
    }

    @Test
    public void validSelfConfigurator() throws Exception {
        assertThat(
            validateSchema(convertYamlFileToJson(this, "validSelfConfig.yml")),
            empty());
    }

    @Test
    public void attributesNotFlattenedToTopLevel() throws Exception {
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
