package io.jenkins.plugins.casc.fetcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.domains.Domain;
import hudson.util.Secret;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;

public class FetchCredentialsChainTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testJenkinsCredentialsOverrideLowerOrdinalProviders() throws Exception {
        CredentialsStore systemStore =
                CredentialsProvider.lookupStores(j.jenkins).iterator().next();
        StringCredentialsImpl jenkinsCred = new StringCredentialsImpl(
                com.cloudbees.plugins.credentials.CredentialsScope.GLOBAL,
                "shared-id",
                "High Priority Jenkins Secret",
                Secret.fromString("WINNER-JENKINS-SECRET"));
        systemStore.addCredentials(Domain.global(), jenkinsCred);

        FetchCredentials credentials = FetchCredentials.resolveAll();
        FetchAuthData.Token tokenAuth = credentials.get("shared-id", FetchAuthData.Token.class);

        assertNotNull(tokenAuth);
        assertEquals(
                "The Jenkins Credentials Provider should take precedence over the environment provider",
                "WINNER-JENKINS-SECRET",
                tokenAuth.getToken());
    }

    @TestExtension
    @SuppressWarnings("unused")
    public static class MockEnvProvider implements FetchCredentialsProvider {
        @SuppressWarnings("unchecked")
        @Override
        public <T extends FetchAuthData> T getCredentials(String credentialId, Class<T> type) {
            if ("shared-id".equals(credentialId) && type == FetchAuthData.Token.class) {
                return (T) (FetchAuthData.Token) () -> "LOSER-ENV-SECRET";
            }
            return null;
        }
    }
}
