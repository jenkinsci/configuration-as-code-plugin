package io.jenkins.plugins.casc;

import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import java.io.BufferedWriter;
import java.io.FileWriter;
import org.junit.Rule;
import org.junit.Test;

import static io.jenkins.plugins.casc.SchemaGeneration.writeJSONSchema;
import static io.jenkins.plugins.casc.misc.Util.convertYamlFileToJson;
import static io.jenkins.plugins.casc.misc.Util.validateSchema;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;

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
            contains("TODO, the message should be about invalid type for numExecutors"));
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
    public void validSelfConfigurator() throws Exception {
        assertThat(
            validateSchema(convertYamlFileToJson(this, "validSelfConfig.yml")),
            empty());
    }

    //    For testing purposes.To be removed
    @Test
    public void writeSchema() throws Exception {
        BufferedWriter writer = new BufferedWriter(new FileWriter("schema.json"));
        writer.write(writeJSONSchema());
        writer.close();
    }
}
