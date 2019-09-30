package io.jenkins.plugins.casc;

import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.Util;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.logging.Logger;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Rule;
import org.junit.Test;

import static io.jenkins.plugins.casc.SchemaGeneration.generateSchema;
import static io.jenkins.plugins.casc.SchemaGeneration.writeJSONSchema;
import static org.junit.Assert.fail;

public class SchemaGenerationTest {

    public static final Logger LOGGER = Logger.getLogger(TokenReloadAction.class.getName());
    private final String failureMessage  = "The YAML file provided for this schema is invalid";

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
            LOGGER.warning(failureMessage);
            fail();
        } catch (ValidationException ve) {
            ve.printStackTrace();
        }
    }

    @Test
    public void rejectsInvalidBaseConfigurator() throws Exception {
        JSONObject schemaObject = generateSchema();
        JSONObject jsonSchema = new JSONObject(
            new JSONTokener(schemaObject.toString()));
        String yamlStringContents = Util.toStringFromYamlFile(this, "invalidBaseConfig.yml");
        JSONObject jsonSubject = new JSONObject(
            new JSONTokener(Util.convertToJson(yamlStringContents)));
        Schema schema = SchemaLoader.load(jsonSchema);
        try {
            schema.validate(jsonSubject);
            LOGGER.warning(failureMessage);
            fail();
        } catch (ValidationException ve) {
            ve.printStackTrace();
        }
    }

    @Test
    public void validJenkinsBaseConfigurator() throws Exception {
        JSONObject schemaObject = generateSchema();
        JSONObject jsonSchema = new JSONObject(
            new JSONTokener(schemaObject.toString()));
        String yamlStringContents = Util.toStringFromYamlFile(this, "validJenkinsBaseConfig.yml");
        JSONObject jsonSubject = new JSONObject(
            new JSONTokener(Util.convertToJson(yamlStringContents)));
        Schema schema = SchemaLoader.load(jsonSchema);
        try {
            schema.validate(jsonSubject);
            LOGGER.warning(failureMessage);
            fail();
        } catch (ValidationException ve) {
            ve.printStackTrace();
        }
    }


    //    For testing purposes.To be removed
    @Test
    public void writeSchema() throws Exception {
        BufferedWriter writer = new BufferedWriter(new FileWriter("schema.json"));
        writer.write(writeJSONSchema());
        writer.close();
    }
 }

