package io.jenkins.plugins.casc;

import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class CredentialsTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @ConfiguredWithCode("GlobalCredentials.yml")
    @Test
    public void testGlobalScopedCredentials() {
        List<StandardUsernamePasswordCredentials> creds = CredentialsProvider.lookupCredentials(StandardUsernamePasswordCredentials.class,Jenkins.getInstanceOrNull(), null, Collections.emptyList());
        assertThat(creds.size(), is(1));
        assertEquals("user1", creds.get(0).getId());
        assertEquals("Administrator", creds.get(0).getUsername());
        assertEquals("secretPassword", creds.get(0).getPassword().getPlainText());

        List<BasicSSHUserPrivateKey> creds2 = CredentialsProvider.lookupCredentials(BasicSSHUserPrivateKey.class,Jenkins.getInstanceOrNull(), null, Collections.emptyList());
        assertThat(creds2.size(), is(1));
        assertEquals("agentuser", creds2.get(0).getUsername());
        assertEquals("password", creds2.get(0).getPassphrase().getPlainText());
        assertEquals("ssh private key used to connect ssh slaves", creds2.get(0).getDescription());
    }


    @ConfiguredWithCode("CredentialsWithDomain.yml")
    @Test
    public void testDomainScopedCredentials() {
        List<StandardUsernamePasswordCredentials> creds = CredentialsProvider.lookupCredentials(StandardUsernamePasswordCredentials.class,Jenkins.getInstanceOrNull(), null, Collections.emptyList());
        assertThat(creds.size(), is(1));
        assertEquals("user1", creds.get(0).getId());
        assertEquals("Administrator", creds.get(0).getUsername());
        assertEquals("secret", creds.get(0).getPassword().getPlainText());
    }

}
