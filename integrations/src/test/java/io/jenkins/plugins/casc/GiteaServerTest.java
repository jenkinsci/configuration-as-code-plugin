package io.jenkins.plugins.casc;

import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import jenkins.model.GlobalConfiguration;
import org.jenkinsci.plugin.gitea.servers.GiteaServer;
import org.jenkinsci.plugin.gitea.servers.GiteaServers;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GiteaServerTest {

    @Rule
    public JenkinsConfiguredWithReadmeRule j = new JenkinsConfiguredWithReadmeRule();

    @Test
    @ConfiguredWithReadme("gitea/README.md")
    public void configure_gitea() {
        final GiteaServers configuration = GlobalConfiguration.all().get(GiteaServers.class);
        assertEquals(configuration.getServers().size(), 1);

        GiteaServer config = configuration.getServers().get(0);
        assertEquals("https://my-scm-url", config.getServerUrl());
        assertEquals("<my-credential-id>", config.getCredentialsId());
        assertEquals("scm", config.getDisplayName());
        assertTrue(config.isManageHooks());
    }
}
