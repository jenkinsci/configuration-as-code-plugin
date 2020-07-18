package io.jenkins.plugins.casc;

import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey;
import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.LoggerRule;

import static io.jenkins.plugins.casc.misc.Util.assertNotInLog;
import static org.hamcrest.CoreMatchers.containsString;
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

    private <T extends Credentials> T getCredentials(Class<T> clazz) {
        List<T> creds = CredentialsProvider.lookupCredentials(
                clazz, Jenkins.getInstanceOrNull(),
                null, Collections.emptyList());
        assertEquals(1, creds.size());
        return (T)creds.get(0);
    }
}
