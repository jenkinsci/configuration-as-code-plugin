package io.jenkins.plugins.casc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.jenkinsci.plugins.pipeline.modeldefinition.shaded.com.fasterxml.jackson.databind.JsonNode;
import org.jenkinsci.plugins.pipeline.modeldefinition.shaded.com.github.fge.jsonschema.main.JsonSchemaFactory;
import org.jenkinsci.plugins.pipeline.modeldefinition.shaded.com.github.fge.jsonschema.main.JsonValidator;
import org.jenkinsci.plugins.pipeline.modeldefinition.shaded.com.github.fge.jsonschema.report.ProcessingReport;
import org.jenkinsci.plugins.pipeline.modeldefinition.shaded.com.github.fge.jsonschema.util.JsonLoader;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static com.gargoylesoftware.htmlunit.HttpMethod.POST;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class SchemaTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("SchemaTest.yml")
    public void schemaConfigurationTest() throws Exception {
        JenkinsRule.WebClient client = j.createWebClient();
        WebRequest request =
                new WebRequest(client.createCrumbedUrl("configuration-as-code/schema"), POST);
        WebResponse response = client.loadWebResponse(request);
        assertThat(response.getStatusCode(), is(200));
        String schema = response.getContentAsString();
        String contents = new String(Files.readAllBytes(Paths.get("./src/test/resources/io/jenkins/plugins/casc/SchemaTest.yml")));
        String json = convertYamlToJson((contents));
        validateJsonData(schema,json);

    }

    String convertYamlToJson(String yaml) throws IOException {
        ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
        Object obj = yamlReader.readValue(yaml, Object.class);

        ObjectMapper jsonWriter = new ObjectMapper();
        return jsonWriter.writeValueAsString(obj);
    }

    private void validateJsonData(final String jsonSchema, final String jsonData) throws IOException {
            final JsonNode data = JsonLoader.fromString(jsonData);
            final JsonNode schema = JsonLoader.fromString(jsonSchema);

            final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
            JsonValidator validator = factory.getValidator();

        ProcessingReport report = null;
        try {
            report = validator.validate(schema, data);
        } catch (Exception e) {
            System.out.println("Invalid Schema");
            e.printStackTrace();
        }
        
        assertTrue(report.isSuccess());
    }
}
