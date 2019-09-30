package io.jenkins.plugins.casc;

import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.Rule;
import org.junit.Test;

import static io.jenkins.plugins.casc.misc.Util.convertYamlFileToJson;
import static io.jenkins.plugins.casc.misc.Util.validateSchema;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SchemaGenerationTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    public void validSchemaShouldSucceed() throws Exception {
        assertTrue(validateSchema(convertYamlFileToJson(this, "validSchemaConfig.yml")));
    }

    @Test
    public void invalidSchemaShouldNotSucceed() throws Exception {
        assertFalse(validateSchema(convertYamlFileToJson(this,"invalidSchemaConfig.yml")));
    }
}
