package io.jenkins.plugins.casc;

import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey;
import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey.DirectEntryPrivateKeySource;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import hudson.plugins.git.GitTool;
import hudson.security.ACL;
import hudson.security.LDAPSecurityRealm;
import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import java.util.Collections;
import java.util.List;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.RuleChain;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author v1v (Victor Martinez)
 */
public class TopReadmeTest {

    @Rule
    public RuleChain chain = RuleChain.outerRule(new EnvironmentVariables()
        .set("SUDO_PASSWORD", "1234")
        .set("SSH_PRIVATE_KEY", "s3cr3t")
        .set("SSH_KEY_PASSWORD", "ABCD"))
        .around(new JenkinsConfiguredWithReadmeRule());

    @Test
    @ConfiguredWithReadme("README.md#0")
    public void configure_demo_first_code_block() throws Exception {
        final Jenkins jenkins = Jenkins.get();
        assertEquals("Jenkins configured automatically by Jenkins Configuration as Code plugin\n\n", jenkins.getSystemMessage());
        final LDAPSecurityRealm securityRealm = (LDAPSecurityRealm) jenkins.getSecurityRealm();
        assertEquals(1, securityRealm.getConfigurations().size());
        assertEquals(50000, jenkins.getSlaveAgentPort());

        assertEquals(1, jenkins.getNodes().size());
        assertEquals("static-agent", jenkins.getNode("static-agent").getNodeName());

        final GitTool.DescriptorImpl gitTool = (GitTool.DescriptorImpl) jenkins.getDescriptor(GitTool.class);
        assertEquals(1, gitTool.getInstallations().length);

        List<BasicSSHUserPrivateKey> sshPrivateKeys = CredentialsProvider.lookupCredentials(
            BasicSSHUserPrivateKey.class, jenkins, ACL.SYSTEM, Collections.emptyList()
        );
        assertThat(sshPrivateKeys, hasSize(1));

        final BasicSSHUserPrivateKey ssh_with_passphrase = sshPrivateKeys.get(0);
        assertThat(ssh_with_passphrase.getPassphrase().getPlainText(), equalTo("ABCD"));

        final DirectEntryPrivateKeySource source = (DirectEntryPrivateKeySource) ssh_with_passphrase.getPrivateKeySource();
        assertThat(source.getPrivateKey().getPlainText(), equalTo("s3cr3t"));
    }

    @Test
    @ConfiguredWithReadme("README.md#1")
    public void configure_demo_second_code_block() throws Exception {
        final Jenkins jenkins = Jenkins.get();
        final LDAPSecurityRealm securityRealm = (LDAPSecurityRealm) jenkins.getSecurityRealm();
        assertEquals(1, securityRealm.getConfigurations().size());
    }
}
