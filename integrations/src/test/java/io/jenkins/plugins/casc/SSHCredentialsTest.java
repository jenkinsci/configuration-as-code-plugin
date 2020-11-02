package io.jenkins.plugins.casc;

import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey;
import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.LoggerRule;
import org.jvnet.hudson.test.TestExtension;

import static io.jenkins.plugins.casc.misc.Util.assertNotInLog;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;


/**
 * Integration tests for the SSH Credentials Plugin.
 */
public class SSHCredentialsTest {

    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();
    public LoggerRule logging = new LoggerRule();

    @Rule
    public RuleChain chain= RuleChain
            .outerRule(logging.record("io.jenkins.plugins.casc.Attribute", Level.INFO).capture(2048))
            .around(j);

    private static final String CREDENTIALS_PASSWORD = "password-of-userid";
    private static final String PRIVATE_KEY = "sp0ds9d+skkfjf";

    @Test
    @ConfiguredWithCode("SSHCredentialsTest.yml")
    @Issue("SECURITY-1279")
    public void shouldNotExportOrLogCredentials() throws Exception {
        StandardUsernamePasswordCredentials creds = getCredentials(StandardUsernamePasswordCredentials.class);
        assertEquals(CREDENTIALS_PASSWORD, creds.getPassword().getPlainText());
        assertNotInLog(logging, CREDENTIALS_PASSWORD);

        BasicSSHUserPrivateKey certKey = getCredentials(BasicSSHUserPrivateKey.class);
        // JENKINS-50181 made getPrivateKey always append a trailing newline.
        assertEquals(PRIVATE_KEY + "\n", certKey.getPrivateKey());
        assertNotInLog(logging, PRIVATE_KEY);

        // Verify that the password does not get exported
        String exportedConfig = j.exportToString(false);
        assertThat("There should be no password in the exported YAML", exportedConfig, not(containsString(CREDENTIALS_PASSWORD)));
        assertThat("There should be no private key in the exported YAML", exportedConfig, not(containsString(PRIVATE_KEY)));
    }

    @Test
    @ConfiguredWithCode("SSHCredentialsTest_Multiline_Key.yml")
    @Issue("https://github.com/jenkinsci/configuration-as-code-plugin/issues/1189")
    public void shouldSupportMultilineCertificates() throws Exception {
        BasicSSHUserPrivateKey certKey = getCredentials(BasicSSHUserPrivateKey.class);
        assertThat("Private key roundtrip failed",
            certKey.getPrivateKey().trim(), equalTo(MySSHKeySecretSource.PRIVATE_SSH_KEY.trim()));
    }

    @Test
    @ConfiguredWithCode("SSHCredentialsTest_Singleline_Key.yml")
    @Issue("https://github.com/jenkinsci/configuration-as-code-plugin/issues/1189")
    public void shouldSupportSinglelineBase64Certificates() throws Exception {
        BasicSSHUserPrivateKey certKey = getCredentials(BasicSSHUserPrivateKey.class);
        assertThat("Private key roundtrip failed",
            certKey.getPrivateKey().trim().replace("\r\n", "\n"), equalTo(MySSHKeySecretSource.PRIVATE_SSH_KEY));
    }

    private <T extends Credentials> T getCredentials(Class<T> clazz) {
        List<T> creds = CredentialsProvider.lookupCredentials(
                clazz, Jenkins.getInstanceOrNull(),
                null, Collections.emptyList());
        assertEquals("There should be only one credential", 1, creds.size());
        return (T)creds.get(0);
    }

    @TestExtension
    public static class MySSHKeySecretSource extends SecretSource {

        private static final String PRIVATE_SSH_KEY =
            "-----BEGIN OPENSSH PRIVATE KEY-----\n" +
            "b3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAAEbm9uZQAAAAAAAAABAAAAMwAAAAtzc2gtZW\n" +
            "QyNTUxOQAAACCYdvz4LdHg0G5KFS8PlauuOwVBms6Y70FaL4JY1YVahgAAAKCjJ1l+oydZ\n" +
            "fgAAAAtzc2gtZWQyNTUxOQAAACCYdvz4LdHg0G5KFS8PlauuOwVBms6Y70FaL4JY1YVahg\n" +
            "AAAEBWrtFZGX1yOg1/esgm34TPE5Zw8EXQ1OuxcgYGIaRRVph2/Pgt0eDQbkoVLw+Vq647\n" +
            "BUGazpjvQVovgljVhVqGAAAAGW9uZW5hc2hldkBMQVBUT1AtMjVLNjVMT1MBAgME\n" +
            "-----END OPENSSH PRIVATE KEY-----";

        // encoded with "base64 -w 0"
        private static final String PRIVATE_SSH_KEY_BASE64 = "LS0tLS1CRUdJTiBPUEVOU1NIIFBSSVZBVEUgS0VZLS0tLS0NCmIzQmxibk56YUMxclpYa3RkakVBQUFBQUJHNXZibVVBQUFBRWJtOXVaUUFBQUFBQUFBQUJBQUFBTXdBQUFBdHpjMmd0WlcNClF5TlRVeE9RQUFBQ0NZZHZ6NExkSGcwRzVLRlM4UGxhdXVPd1ZCbXM2WTcwRmFMNEpZMVlWYWhnQUFBS0NqSjFsK295ZFoNCmZnQUFBQXR6YzJndFpXUXlOVFV4T1FBQUFDQ1lkdno0TGRIZzBHNUtGUzhQbGF1dU93VkJtczZZNzBGYUw0SlkxWVZhaGcNCkFBQUVCV3J0RlpHWDF5T2cxL2VzZ20zNFRQRTVadzhFWFExT3V4Y2dZR0lhUlJWcGgyL1BndDBlRFFia29WTHcrVnE2NDcNCkJVR2F6cGp2UVZvdmdsalZoVnFHQUFBQUdXOXVaVzVoYzJobGRrQk1RVkJVVDFBdE1qVkxOalZNVDFNQkFnTUUNCi0tLS0tRU5EIE9QRU5TU0ggUFJJVkFURSBLRVktLS0tLQ0K";

        @Override
        public Optional<String> reveal(String secret) throws IOException {
            if (secret.equals("MY_PRIVATE_KEY")) {
                return Optional.of(PRIVATE_SSH_KEY);
            }
            if (secret.equals("SSH_AGENT_PRIVATE_KEY_BASE64")) {
                return Optional.of(PRIVATE_SSH_KEY_BASE64);
            }

            return Optional.empty();
        }
    }
}
