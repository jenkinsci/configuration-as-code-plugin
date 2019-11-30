package io.jenkins.plugins.casc;

import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.Rule;
import org.junit.Test;

import static io.jenkins.plugins.casc.SchemaGeneration.removeHtmlTags;
import static io.jenkins.plugins.casc.SchemaGeneration.retrieveDocStringFromAttribute;
import static io.jenkins.plugins.casc.misc.Util.convertYamlFileToJson;
import static io.jenkins.plugins.casc.misc.Util.validateSchema;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

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
