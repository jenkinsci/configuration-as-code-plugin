package io.jenkins.plugins.casc;

import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey;
import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey.DirectEntryPrivateKeySource;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.CertificateCredentials;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import hudson.security.ACL;
import io.jenkins.plugins.casc.impl.configurators.DataBoundConfigurator;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.model.CNode;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.RuleChain;
import org.jvnet.hudson.test.LoggerRule;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class SystemCredentialsTest {

    @Rule
    public LoggerRule log = new LoggerRule()
        .recordPackage(DataBoundConfigurator.class, Level.INFO)
        .capture(100);

    @Rule
    public RuleChain chain = RuleChain.outerRule(new EnvironmentVariables()
            .set("SUDO_PASSWORD", "1234")
            .set("SSH_PRIVATE_KEY", "s3cr3t")
            .set("SSH_KEY_PASSWORD", "ABCD"))
            .around(log)
            .around(new JenkinsConfiguredWithCodeRule());


    @Test
    @ConfiguredWithCode("SystemCredentialsTest.yml")
    public void configure_system_credentials() throws Exception {
        Jenkins jenkins = Jenkins.get();

        List<UsernamePasswordCredentials> ups = CredentialsProvider.lookupCredentials(
                UsernamePasswordCredentials.class, jenkins, ACL.SYSTEM, Collections.emptyList()
        );
        assertThat(ups, hasSize(1));
        final UsernamePasswordCredentials up = ups.get(0);
        assertThat(up.getPassword().getPlainText(), equalTo("1234"));

        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        final ConfigurationContext context = new ConfigurationContext(registry);
        final CNode node = context.lookup(up.getClass()).describe(up, context);
        assertThat(node.asMapping().getScalarValue("password"), not(equals("1234")));


        List<CertificateCredentials> certs = CredentialsProvider.lookupCredentials(
                CertificateCredentials.class, jenkins, ACL.SYSTEM, Collections.emptyList()
        );
        assertThat(certs, hasSize(1));
        assertThat(certs.get(0).getPassword().getPlainText(), equalTo("ABCD"));

        List<BasicSSHUserPrivateKey> sshPrivateKeys = CredentialsProvider.lookupCredentials(
                BasicSSHUserPrivateKey.class, jenkins, ACL.SYSTEM, Collections.emptyList()
        );
        assertThat(sshPrivateKeys, hasSize(2));
        final BasicSSHUserPrivateKey ssh_with_passphrase = sshPrivateKeys.stream()
                .filter(k -> k.getId().equals("ssh_with_passphrase_provided"))
                .findFirst().orElseThrow(AssertionError::new);
        assertThat(ssh_with_passphrase.getPassphrase().getPlainText(), equalTo("ABCD"));

        final DirectEntryPrivateKeySource source = (DirectEntryPrivateKeySource) ssh_with_passphrase.getPrivateKeySource();
        assertThat(source.getPrivateKey(), equalTo("s3cr3t"));


        // credentials should not appear in plain text in log
        for (LogRecord logRecord : log.getRecords()) {
            assertThat(logRecord.getMessage(), not(containsString("1234")));
            assertThat(logRecord.getMessage(), not(containsString("ABCD")));
        }


    }
}
