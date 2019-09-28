package io.jenkins.plugins.casc;

import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.Util;
import java.util.logging.Logger;
import org.dom4j.datatype.InvalidSchemaException;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Rule;
import org.junit.Test;

import static io.jenkins.plugins.casc.SchemaGeneration.generateSchema;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SchemaGenerationTest {

    private static final Logger LOGGER = Logger.getLogger(TokenReloadAction.class.getName());
    private final String failureMessage  = "The YAML file provided for this schema is invalid";

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    public void validSchemaShouldSucceed() {
        Schema schema = returnSchema();
        assertTrue(validateSchema(schema, returnJSONSubject("validSchemaConfig.yml")));
    }

    @Test
    public void invalidSchemaShouldNotSucceed() {
        Schema schema = returnSchema();
        assertFalse(validateSchema(schema, returnJSONSubject("invalidSchemaConfig.yml")));

    }

    private Schema returnSchema() {
        JSONObject schemaObject = generateSchema();
        JSONObject jsonSchema = new JSONObject(
            new JSONTokener(schemaObject.toString()));
        return SchemaLoader.load(jsonSchema);
    }

    private JSONObject returnJSONSubject(String yamlFile)  {
        String yamlStringContents = null;
        try {
            yamlStringContents = Util.toStringFromYamlFile(this, yamlFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new JSONObject(
            new JSONTokener(Util.convertToJson(yamlStringContents)));
    }

    private boolean validateSchema(Schema schema, JSONObject jsonSubject) {
        try {
            schema.validate(jsonSubject);
        } catch (InvalidSchemaException ie) {
            LOGGER.warning(failureMessage + ie);
            return false;
        }
        return true;
    }
}
