package org.jenkinsci.plugins.casc;

import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.CertificateCredentials;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.casc.misc.ConfiguredWithCode;
import org.jenkinsci.plugins.casc.misc.EnvVarsRule;
import org.jenkinsci.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class SystemCredentialsTest {

    @Rule
    public RuleChain chain = RuleChain.outerRule(new EnvVarsRule()
            .env("SUDO_PASSWORD", "1234")
            .env("SSH_PRIVATE_KEY", "s3cr3t")
            .env("SSH_KEY_PASSWORD", "ABCD"))
            .around(new JenkinsConfiguredWithCodeRule());

    @Test
    @ConfiguredWithCode("SystemCredentialsTest.yml")
    public void configure_system_credentials() throws Exception {
        Jenkins jenkins = Jenkins.getInstance();
        List<UsernamePasswordCredentials> ups = CredentialsProvider.lookupCredentials(
                UsernamePasswordCredentials.class, jenkins, ACL.SYSTEM, Collections.emptyList()
        );
        assertThat(ups, hasSize(1));
        assertThat(ups.get(0).getPassword().getPlainText(), equalTo("1234"));

        List<CertificateCredentials> certs = CredentialsProvider.lookupCredentials(
                CertificateCredentials.class, jenkins, ACL.SYSTEM, Collections.emptyList()
        );
        assertThat(certs, hasSize(1));
        assertThat(certs.get(0).getPassword().getPlainText(), equalTo("ABCD"));

        List<BasicSSHUserPrivateKey> sshPrivateKeys = CredentialsProvider.lookupCredentials(BasicSSHUserPrivateKey.class, j.jenkins, ACL.SYSTEM, Collections.EMPTY_LIST);
        assertThat(sshPrivateKeys, hasSize(2));
        assertThat(sshPrivateKeys.get(0).getPassphrase().getPlainText(), equalTo("ABCD"));
    }
}
