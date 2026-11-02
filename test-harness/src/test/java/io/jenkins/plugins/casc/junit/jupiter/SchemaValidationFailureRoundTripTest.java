package io.jenkins.plugins.casc.junit.jupiter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import io.jenkins.plugins.casc.misc.junit.jupiter.AbstractRoundTripTest;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Checks that a round trip test with schema validation enabled fails on a config that does not match the schema,
 * and that the failure message lists the schema errors.
 */
@WithJenkins
class SchemaValidationFailureRoundTripTest {

    @Test
    void invalidConfigFailsWithSchemaErrors(JenkinsRule r) {
        AssertionError error = assertThrows(AssertionError.class, () -> new InvalidSchemaRoundTrip().roundTripTest(r));
        assertThat(error.getMessage(), containsString("invalidSchemaRoundTrip.yml does not match the JSON schema"));
        assertThat(error.getMessage(), containsString("#/jenkins/numExecutors: expected type: Integer, found: String"));
    }

    /**
     * Nested so that it is not picked up and run on its own.
     */
    static class InvalidSchemaRoundTrip extends AbstractRoundTripTest {

        @Override
        protected boolean validateSchema() {
            return true;
        }

        @Override
        protected String configResource() {
            return "invalidSchemaRoundTrip.yml";
        }

        @Override
        protected void assertConfiguredAsExpected(JenkinsRule j, String configContent) {
            fail("Config should have been rejected by schema validation before being applied");
        }

        @Override
        protected String stringInLogExpected() {
            return "Configured by invalid schema round trip";
        }
    }
}
