package io.jenkins.plugins.casc;

import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.Util;
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
    public void invalidJenkinsDataForBaseConfig() throws Exception {
        JSONObject schemaObject = generateSchema();
        JSONObject jsonSchema = new JSONObject(
            new JSONTokener(schemaObject.toString()));
        String yamlStringContents = Util.toStringFromYamlFile(this, "invalidDataFormat.yml");
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
    public void validJenkinsDataForBaseConfig() throws Exception {
        JSONObject schemaObject = generateSchema();
        JSONObject jsonSchema = new JSONObject(
            new JSONTokener(schemaObject.toString()));
        String yamlStringContents = Util.toStringFromYamlFile(this, "validDataFormat.yml");
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
    public void validToolDataForBaseConfig() throws Exception {
        JSONObject schemaObject = generateSchema();
        JSONObject jsonSchema = new JSONObject(
            new JSONTokener(schemaObject.toString()));
        String yamlStringContents = Util.toStringFromYamlFile(this, "validToolBaseConfig.yml");
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
    public void invalidToolDataForBaseConfig() throws Exception {
        JSONObject schemaObject = generateSchema();
        JSONObject jsonSchema = new JSONObject(
            new JSONTokener(schemaObject.toString()));
        String yamlStringContents = Util.toStringFromYamlFile(this, "invalidToolBaseConfig.yml");
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
    public void invalidEmptyToolForBaseConfig() throws Exception {
        JSONObject schemaObject = generateSchema();
        JSONObject jsonSchema = new JSONObject(
            new JSONTokener(schemaObject.toString()));
        String yamlStringContents = Util.toStringFromYamlFile(this, "emptyToolBaseConfig.yml");
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
    public void testRootConfiguratorGeneration() throws Exception {
        rootConfigGeneration();
    }

 }

