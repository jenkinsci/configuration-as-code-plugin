package io.jenkins.plugins.casc;

import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.Util;
import java.util.logging.Logger;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Rule;
import org.junit.Test;

import static io.jenkins.plugins.casc.SchemaGeneration.generateSchema;
import static org.junit.Assert.fail;

public class SchemaGenerationTest {

    public static final Logger LOGGER = Logger.getLogger(TokenReloadAction.class.getName());
    private final String failureMessage  = "The YAML file provided for this schema is invalid";

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    public void validSchemaShouldSucceed() throws Exception {
        Schema schema = returnSchema();
        try {
            validateSchema(schema, returnJSONSubject("validSchemaConfig.yml"));
        } catch (Exception e) {
            LOGGER.warning(failureMessage);
            fail(e.getMessage());
        }
    }

    @Test
    public void invalidSchemaShouldNotSucceed() throws Exception {
        Schema schema = returnSchema();
        try {
            validateSchema(schema, returnJSONSubject("invalidSchemaConfig.yml"));
        } catch (Exception e) {
            LOGGER.warning(failureMessage);
            fail(e.getMessage());
        }
    }

    private Schema returnSchema() {
        JSONObject schemaObject = generateSchema();
        JSONObject jsonSchema = new JSONObject(
            new JSONTokener(schemaObject.toString()));
        Schema schema = SchemaLoader.load(jsonSchema);
        return schema;
    }

    private JSONObject returnJSONSubject(String yamlFile) throws Exception {
        String yamlStringContents = Util.toStringFromYamlFile(this, yamlFile);
        JSONObject jsonSubject = new JSONObject(
            new JSONTokener(Util.convertToJson(yamlStringContents)));
        return jsonSubject;
    }

    private void validateSchema(Schema schema, JSONObject jsonSubject) throws Exception {
        schema.validate(jsonSubject);
    }
}
