package io.jenkins.plugins.casc;

import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author <a href="mailto:ohad.david@gmail.com">Ohad David</a>
 */
public class ScriptApprovalConfiguratorTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("ScriptApprovalConfiguratorTest.yml")
    public void should_configure_maven_tools_and_global_config() {
        final ScriptApproval scriptApproval = ScriptApproval.get();
        String[] signatures = scriptApproval.getApprovedSignatures();
        Assert.assertArrayEquals(signatures, new String[]{
            "method java.net.URI getHost",
            "method java.net.URI getPort",
            "new java.net.URI java.lang.String"
        });
    }
}
