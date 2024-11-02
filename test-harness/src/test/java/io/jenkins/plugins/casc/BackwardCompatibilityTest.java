package io.jenkins.plugins.casc;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import hudson.model.Node;
import hudson.util.VersionNumber;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import java.util.List;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class BackwardCompatibilityTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("BackwardCompatibilityTest.yml")
    public void should_accept_legacy_symbols_on_descriptors() {

        final List<Node> nodes = j.jenkins.getNodes();
        System.out.println(nodes);
        assertNotNull(j.jenkins.getNode("foo"));
        assertNotNull(j.jenkins.getNode("bar"));
        assertNotNull(j.jenkins.getNode("qix"));

        final List<ObsoleteConfigurationMonitor.Error> errors =
                ObsoleteConfigurationMonitor.get().getErrors();
        String expected = format("'DumbSlave' is obsolete, please use '%s'", agentNameToCompareAgainst());
        assertEquals(expected, errors.get(0).message);
    }

    private static String agentNameToCompareAgainst() {
        VersionNumber currentVersion = Jenkins.getStoredVersion();
        if (currentVersion == null) {
            throw new IllegalArgumentException("Couldn't get jenkins version");
        }
        return "permanent";
    }
}
