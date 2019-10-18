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
import static io.jenkins.plugins.casc.SchemaGeneration.removeHtmlTags;
import static io.jenkins.plugins.casc.SchemaGeneration.retrieveDocStringFromAttribute;
import static io.jenkins.plugins.casc.misc.Util.toStringFromYamlFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class SchemaGenerationTest {
    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    public void validSchemaShouldSucceed() throws Exception {
        JSONObject schemaObject = generateSchema();
        JSONObject jsonSchema = new JSONObject(
            new JSONTokener(schemaObject.toString()));
        String yamlStringContents = toStringFromYamlFile(this, "validSchemaConfig.yml");
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
        String yamlStringContents = toStringFromYamlFile(this, "invalidSchemaConfig.yml");
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
    public void testRetrieveDocStringFromAttribute() {
        String expectedDocString = "If checked, this will allow users who are not authenticated to access Jenkins in a read-only mode.";
        String actualDocString = retrieveDocStringFromAttribute(hudson.security.FullControlOnceLoggedInAuthorizationStrategy.class, "allowAnonymousRead");
        assertEquals(expectedDocString,actualDocString);
    }

    @Test
    public void testRemoveHtmlTagRegex() {
        String htmlTagString = "<div> If checked, this will allow users who are not authenticated to access Jenkins in a read-only mode.</div>";
        String expectedString = "If checked, this will allow users who are not authenticated to access Jenkins in a read-only mode.";
        String actualString = removeHtmlTags(htmlTagString);
        assertEquals(expectedString, actualString);
    }

}
