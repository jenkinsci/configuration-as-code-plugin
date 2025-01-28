package io.jenkins.plugins.casc;

import static io.jenkins.plugins.casc.SchemaGeneration.removeHtmlTags;
import static io.jenkins.plugins.casc.SchemaGeneration.retrieveDocStringFromAttribute;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import org.junit.jupiter.api.Test;

@WithJenkinsConfiguredWithCode
class SchemaGenerationSanitisationTest {

    @Test
    void testRetrieveDocStringFromAttribute(JenkinsConfiguredWithCodeRule j) {
        String expectedDocString =
                "If checked, this will allow users who are not authenticated to access Jenkins\n  in a read-only mode.";
        String actualDocString = retrieveDocStringFromAttribute(
                hudson.security.FullControlOnceLoggedInAuthorizationStrategy.class, "allowAnonymousRead");
        assertEquals(expectedDocString, actualDocString);
    }

    @Test
    void testRemoveHtmlTagRegex(JenkinsConfiguredWithCodeRule j) {
        String htmlTagString =
                "<div> If checked, this will allow users who are not authenticated to access Jenkins in a read-only mode.</div>";
        String expectedString =
                "If checked, this will allow users who are not authenticated to access Jenkins in a read-only mode.";
        String actualString = removeHtmlTags(htmlTagString);
        assertEquals(expectedString, actualString);
    }
}
