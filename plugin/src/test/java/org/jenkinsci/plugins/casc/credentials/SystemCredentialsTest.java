package org.jenkinsci.plugins.casc.credentials;

import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.CertificateCredentials;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.casc.ConfigurationContext;
import org.jenkinsci.plugins.casc.ConfiguratorRegistry;
import org.jenkinsci.plugins.casc.impl.configurators.DataBoundConfigurator;
import org.jenkinsci.plugins.casc.misc.ConfiguredWithCode;
import org.jenkinsci.plugins.casc.misc.EnvVarsRule;
import org.jenkinsci.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.jenkinsci.plugins.casc.model.CNode;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.jvnet.hudson.test.LoggerRule;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class SystemCredentialsTest {

    @Rule
    public LoggerRule log = new LoggerRule()
        .recordPackage(DataBoundConfigurator.class, Level.INFO)
        .capture(100);

    @Rule
    public RuleChain chain = RuleChain.outerRule(new EnvVarsRule()
            .env("SUDO_PASSWORD", "1234")
            .env("SSH_PRIVATE_KEY", "s3cr3t")
            .env("SSH_KEY_PASSWORD", "ABCD"))
            .around(log)
            .around(new JenkinsConfiguredWithCodeRule());


    @Test
    @ConfiguredWithCode("SystemCredentialsTest.yml")
    public void configure_system_credentials() throws Exception {
        Jenkins jenkins = Jenkins.getInstance();

        List<UsernamePasswordCredentials> ups = CredentialsProvider.lookupCredentials(
                UsernamePasswordCredentials.class, jenkins, ACL.SYSTEM, Collections.emptyList()
        );
        assertThat(ups, hasSize(1));
        final UsernamePasswordCredentials up = ups.get(0);
        assertThat(up.getPassword().getPlainText(), equalTo("1234"));

        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        final ConfigurationContext context = new ConfigurationContext(registry);
        final CNode node = context.lookup(up.getClass()).describe(up, context);
        assertEquals("1234", node.asMapping().getScalarValue("password"));


        List<CertificateCredentials> certs = CredentialsProvider.lookupCredentials(
                CertificateCredentials.class, jenkins, ACL.SYSTEM, Collections.emptyList()
        );
        assertThat(certs, hasSize(1));
        assertThat(certs.get(0).getPassword().getPlainText(), equalTo("ABCD"));

        List<BasicSSHUserPrivateKey> sshPrivateKeys = CredentialsProvider.lookupCredentials(
                BasicSSHUserPrivateKey.class, jenkins, ACL.SYSTEM, Collections.emptyList()
        );
        assertThat(sshPrivateKeys, hasSize(2));
        assertThat(sshPrivateKeys.get(0).getPassphrase().getPlainText(), equalTo("ABCD"));

        // credentials should not appear in plain text in log
        for (LogRecord logRecord : log.getRecords()) {
            assertThat(logRecord.getMessage(), not(containsString("1234")));
            assertThat(logRecord.getMessage(), not(containsString("ABCD")));
        }


    }
}
