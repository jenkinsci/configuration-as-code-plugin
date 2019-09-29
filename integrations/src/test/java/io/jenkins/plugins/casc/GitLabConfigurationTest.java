package io.jenkins.plugins.casc;

import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.domains.DomainCredentials;
import com.dabsquared.gitlabjenkins.connection.GitLabApiToken;
import com.dabsquared.gitlabjenkins.connection.GitLabConnection;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionConfig;
import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import java.util.List;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.RuleChain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="osdomin@yahoo.es">osdomin</a>
 */
public class GitLabConfigurationTest {

    @Rule
    public RuleChain chain = RuleChain.outerRule(new EnvironmentVariables()
        .set("BIND_TOKEN", "qwertyuiopasdfghjklzxcvbnm"))
        .around(new JenkinsConfiguredWithReadmeRule());

    @Test
    @ConfiguredWithReadme("gitlab/README.md")
    public void configure_gitlab_api_token() throws Exception {
        SystemCredentialsProvider systemCreds = SystemCredentialsProvider.getInstance();
        List<DomainCredentials> domainCredentials = systemCreds.getDomainCredentials();
        assertEquals(1, domainCredentials.size());
        final DomainCredentials gitLabCredential = domainCredentials.get(0);
        assertEquals(Domain.global(), gitLabCredential.getDomain());
        assertEquals(1, gitLabCredential.getCredentials().size());
        final GitLabApiToken apiToken = (GitLabApiToken)gitLabCredential.getCredentials().get(0);
        assertEquals("gitlab_token", apiToken.getId());
        assertEquals("qwertyuiopasdfghjklzxcvbnm", apiToken.getApiToken().getPlainText());
        assertEquals("Gitlab Token", apiToken.getDescription());
    }
    @Test
    @ConfiguredWithReadme("gitlab/README.md")
    public void configure_gitlab_connection() throws Exception {
        final Jenkins jenkins = Jenkins.get();
        final GitLabConnectionConfig gitLabConnections = jenkins.getDescriptorByType(GitLabConnectionConfig.class);
        assertEquals(1, gitLabConnections.getConnections().size());
        final GitLabConnection gitLabConnection = gitLabConnections.getConnections().get(0);
        assertEquals("gitlab_token", gitLabConnection.getApiTokenId());
        assertEquals("my_gitlab_server", gitLabConnection.getName());
        assertEquals("autodetect", gitLabConnection.getClientBuilderId());
        assertEquals("https://gitlab.com/", gitLabConnection.getUrl());
        assertEquals(20, gitLabConnection.getConnectionTimeout());
        assertEquals(10, gitLabConnection.getReadTimeout());
        assertTrue(gitLabConnection.isIgnoreCertificateErrors());
    }
}
