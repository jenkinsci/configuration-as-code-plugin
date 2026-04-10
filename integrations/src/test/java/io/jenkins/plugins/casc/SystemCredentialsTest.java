package io.jenkins.plugins.casc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey;
import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey.DirectEntryPrivateKeySource;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.CertificateCredentials;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.impl.CertificateCredentialsImpl;
import com.cloudbees.plugins.credentials.impl.CertificateCredentialsImpl.UploadedKeyStoreSource;
import hudson.security.ACL;
import io.jenkins.plugins.casc.impl.configurators.DataBoundConfigurator;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.model.CNode;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.RuleChain;
import org.jvnet.hudson.test.LoggerRule;

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
                    .set("SSH_KEY_PASSWORD", "123456")
                    .set("CERTIFICATE_BASE64", getBase64Keystore()))
            .around(log)
            .around(new JenkinsConfiguredWithCodeRule());

    @Test
    @ConfiguredWithCode("SystemCredentialsTest.yml")
    public void configure_system_credentials() throws Exception {
        Jenkins jenkins = Jenkins.get();

        List<UsernamePasswordCredentials> ups = CredentialsProvider.lookupCredentials(
                UsernamePasswordCredentials.class, jenkins, ACL.SYSTEM, Collections.emptyList());
        assertThat(ups, hasSize(1));
        final UsernamePasswordCredentials up = ups.get(0);
        assertThat(up.getPassword().getPlainText(), equalTo("1234"));

        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        final ConfigurationContext context = new ConfigurationContext(registry);
        final CNode node = context.lookup(up.getClass()).describe(up, context);
        assertThat(node.asMapping().getScalarValue("password"), not(equals("1234")));

        List<CertificateCredentials> certs = CredentialsProvider.lookupCredentials(
                CertificateCredentials.class, jenkins, ACL.SYSTEM, Collections.emptyList());
        assertThat(certs, hasSize(1));

        CertificateCredentialsImpl certImpl = (CertificateCredentialsImpl) certs.get(0);
        assertThat(certImpl.getId(), equalTo("uploaded_certificate"));
        assertThat(certImpl.getPassword().getPlainText(), equalTo("123456"));
        assertThat(certImpl.getKeyStoreSource(), notNullValue());
        assertThat(certImpl.getKeyStoreSource(), instanceOf(UploadedKeyStoreSource.class));
        assertThat(certImpl.getKeyStore(), notNullValue());
        assertThat(certImpl.getKeyStore().size(), greaterThan(0));

        UploadedKeyStoreSource keyStoreSource = (UploadedKeyStoreSource) certImpl.getKeyStoreSource();
        assertThat(keyStoreSource.getUploadedKeystore(), notNullValue());

        byte[] expectedBytes = getRawKeystoreBytes();
        byte[] actualBytes = keyStoreSource.getUploadedKeystore().getPlainData();
        assertThat("The bytes in Jenkins should be identical to the source file", actualBytes, equalTo(expectedBytes));

        Enumeration<String> aliases = certImpl.getKeyStore().aliases();
        assertThat("Keystore should not be empty", aliases.hasMoreElements(), equalTo(true));

        String firstAlias = aliases.nextElement();
        assertThat("Alias should contain a valid key", certImpl.getKeyStore().isKeyEntry(firstAlias), equalTo(true));

        List<BasicSSHUserPrivateKey> sshPrivateKeys = CredentialsProvider.lookupCredentials(
                BasicSSHUserPrivateKey.class, jenkins, ACL.SYSTEM, Collections.emptyList());
        assertThat(sshPrivateKeys, hasSize(1));

        final BasicSSHUserPrivateKey ssh_with_passphrase = sshPrivateKeys.get(0);
        assertThat(ssh_with_passphrase.getPassphrase().getPlainText(), equalTo("123456"));

        final DirectEntryPrivateKeySource source =
                (DirectEntryPrivateKeySource) ssh_with_passphrase.getPrivateKeySource();
        assertThat(source.getPrivateKey().getPlainText(), equalTo("s3cr3t"));

        // credentials should not appear in plain text in log
        for (LogRecord logRecord : log.getRecords()) {
            assertThat(logRecord.getMessage(), not(containsString("1234")));
            assertThat(logRecord.getMessage(), not(containsString("123456")));
        }
    }

    private static String getBase64Keystore() {
        return Base64.getEncoder().encodeToString(getRawKeystoreBytes());
    }

    private static byte[] getRawKeystoreBytes() {
        try {
            URL res = SystemCredentialsTest.class.getResource("test.p12");
            if (res == null) {
                throw new IllegalStateException("Cannot find test.p12 on classpath");
            }
            return Files.readAllBytes(Paths.get(res.toURI()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to read test.p12 from classpath", e);
        }
    }
}
