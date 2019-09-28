package io.jenkins.plugins.casc;

import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.Util;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SchemaGenerationTest {
    
    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    public void validSchemaShouldSucceed() {
        assertTrue(Util.validateSchema(returnJSONSubject("validSchemaConfig.yml")));
    }

    @Test
    public void invalidSchemaShouldNotSucceed() {
        assertFalse(Util.validateSchema(returnJSONSubject("invalidSchemaConfig.yml")));

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
}
