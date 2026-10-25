package io.jenkins.plugins.casc.junit.jupiter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jenkins.plugins.casc.misc.junit.jupiter.AbstractRoundTripTest;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * Runs the full round trip with schema validation enabled on a config that matches the schema.
 */
class SchemaValidatingRoundTripTest extends AbstractRoundTripTest {

    private static final String MESSAGE = "Configured by schema validated round trip";

    @Override
    protected boolean validateSchema() {
        return true;
    }

    @Override
    protected String configResource() {
        return "validSchemaRoundTrip.yml";
    }

    @Override
    protected void assertConfiguredAsExpected(JenkinsRule j, String configContent) {
        assertEquals(MESSAGE, j.jenkins.getSystemMessage());
    }

    @Override
    protected String stringInLogExpected() {
        return MESSAGE;
    }
}
