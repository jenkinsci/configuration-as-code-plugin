package io.jenkins.plugins.casc;


import hudson.security.HudsonPrivateSecurityRealm;
import io.jenkins.plugins.casc.impl.configurators.HeteroDescribableConfigurator;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.Util;
import io.jenkins.plugins.casc.model.CNode;
import jenkins.model.Jenkins;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Rule;
import org.junit.Test;

import static io.jenkins.plugins.casc.SchemaGeneration.generateSchema;
import static io.jenkins.plugins.casc.SchemaGeneration.rootConfigGeneration;
import static org.junit.Assert.fail;

public class SchemaGenerationTest {
    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    public void validSchemaShouldSucceed() throws Exception {
        JSONObject schemaObject = generateSchema();
        JSONObject jsonSchema = new JSONObject(
            new JSONTokener(schemaObject.toString()));
        String yamlStringContents = Util.toStringFromYamlFile(this, "validSchemaConfig.yml");
        JSONObject jsonSubject = new JSONObject(
            new JSONTokener(Util.convertToJson(yamlStringContents)));
        Schema schema = SchemaLoader.load(jsonSchema);

        try {
            schema.validate(jsonSubject);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void invalidSchemaShouldNotSucceed() throws Exception {
        JSONObject schemaObject = generateSchema();
        JSONObject jsonSchema = new JSONObject(
            new JSONTokener(schemaObject.toString()));
        String yamlStringContents = Util.toStringFromYamlFile(this, "invalidSchemaConfig.yml");
        JSONObject jsonSubject = new JSONObject(
            new JSONTokener(Util.convertToJson(yamlStringContents)));
        Schema schema = SchemaLoader.load(jsonSchema);
        try {
            schema.validate(jsonSubject);
            fail();
        } catch (ValidationException ve) {
            ve.printStackTrace();
        }
    }

    @Test
    public void describeStructTest() throws Exception {
         rootConfigGeneration();
    }

    @Test
    public void invalidJenkinsDataForBaseConfig() throws Exception {
        String yamlStringContents = Util.toStringFromYamlFile(this, "invalidDataFormat.yml");
    }

    @Test
    public void validJenkinsDataForBaseConfig() throws Exception {
        String yamlStringContents = Util.toStringFromYamlFile(this, "validDataFormat.yml");
    }

    @Test
    public void validToolDataForBaseConfig() throws Exception {
        String yamlStringContents = Util.toStringFromYamlFile(this, "validToolBaseConfig.yml");
    }

    @Test
    public void invalidToolDataForBaseConfig() throws Exception {
        String yamlStringContents = Util.toStringFromYamlFile(this, "invalidToolBaseConfig.yml");
    }

    @Test
    public void invalidEmptyToolForBaseConfig() throws Exception {
        String yamlStringContents = Util.toStringFromYamlFile(this, "emptyToolBaseConfig.yml");
    }


    private void validateYamlAgainstSchema(String yamlString) {

    }
 }
