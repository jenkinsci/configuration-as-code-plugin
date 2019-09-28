package io.jenkins.plugins.casc;

import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.Util;
import java.util.logging.Logger;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Rule;
import org.junit.Test;

import static io.jenkins.plugins.casc.misc.Util.validateSchema;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SchemaGenerationTest {

    private static final Logger LOGGER = Logger.getLogger(SchemaGenerationTest.class.getName());
    private static final String stringToYAMLConversionFailure  = "Error Converting the YAML file to String";

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    public void validSchemaShouldSucceed() {
        assertTrue(validateSchema(convertYamlFileToJson("validSchemaConfig.yml")));
    }

    @Test
    public void invalidSchemaShouldNotSucceed() {
        assertFalse(validateSchema(convertYamlFileToJson("invalidSchemaConfig.yml")));

    }

    private JSONObject convertYamlFileToJson(String yamlFile)  {
        String yamlStringContents = null;
        try {
            yamlStringContents = Util.toStringFromYamlFile(this, yamlFile);
        } catch (Exception e) {
            LOGGER.warning(stringToYAMLConversionFailure);
        }
        return new JSONObject(
            new JSONTokener(Util.convertToJson(yamlStringContents)));
    }
}
