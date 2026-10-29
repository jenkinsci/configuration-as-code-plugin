package io.jenkins.plugins.casc;

import static org.junit.Assert.assertEquals;

import io.jenkins.plugins.casc.misc.RoundTripAbstractTest;
import org.jvnet.hudson.test.RestartableJenkinsRule;

/**
 * JUnit 4 counterpart of {@code io.jenkins.plugins.casc.junit.jupiter.SchemaValidatingRoundTripTest}.
 */
@SuppressWarnings("deprecation")
public class SchemaValidatingRoundTripJUnit4Test extends RoundTripAbstractTest {

    private static final String MESSAGE = "Configured by JUnit 4 schema validated round trip";

    @Override
    protected boolean validateSchema() {
        return true;
    }

    @Override
    protected String configResource() {
        return "validSchemaRoundTripJUnit4.yml";
    }

    @Override
    protected void assertConfiguredAsExpected(RestartableJenkinsRule j, String configContent) {
        assertEquals(MESSAGE, j.j.jenkins.getSystemMessage());
    }

    @Override
    protected String stringInLogExpected() {
        return MESSAGE;
    }
}
