package io.jenkins.plugins.casc;

import com.nirima.jenkins.plugins.docker.DockerCloud;
import hudson.model.Node.Mode;
import hudson.plugins.git.GitTool;
import hudson.security.FullControlOnceLoggedInAuthorizationStrategy;
import hudson.tasks.Mailer;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import java.util.List;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.libs.GlobalLibraries;
import org.jenkinsci.plugins.workflow.libs.LibraryConfiguration;
import org.jfrog.hudson.ArtifactoryBuilder;
import org.jfrog.hudson.ArtifactoryServer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.RuleChain;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * @author v1v (Victor Martinez)
 */
public class JenkinsDemoTest {

    @Rule
    public RuleChain chain = RuleChain.outerRule(new EnvironmentVariables()
        .set("ARTIFACTORY_PASSWORD", "password123"))
        .around(new JenkinsConfiguredWithCodeRule());

    @Test
    @ConfiguredWithCode("jenkins/jenkins.yaml")
    public void configure_demo_yaml() throws Exception {
        final Jenkins jenkins = Jenkins.get();
        assertEquals("Jenkins configured automatically by Jenkins Configuration as Code plugin\n\n", jenkins.getSystemMessage());
        assertEquals(5, jenkins.getNumExecutors());
        assertEquals(2, jenkins.getScmCheckoutRetryCount());
        assertEquals(Mode.NORMAL, jenkins.getMode());
        assertEquals("https://ci.example.com/", jenkins.getRootUrl());

        final FullControlOnceLoggedInAuthorizationStrategy strategy = (FullControlOnceLoggedInAuthorizationStrategy) jenkins.getAuthorizationStrategy();
        assertFalse(strategy.isAllowAnonymousRead());

        final DockerCloud docker = DockerCloud.getCloudByName("docker");
        assertNotNull(docker);
        assertNotNull(docker.getDockerApi());
        assertNotNull(docker.getDockerApi().getDockerHost());
        assertEquals("unix:///var/run/docker.sock", docker.getDockerApi().getDockerHost().getUri());

        final GitTool.DescriptorImpl gitTool = (GitTool.DescriptorImpl) jenkins.getDescriptor(GitTool.class);
        assertEquals(1, gitTool.getInstallations().length);

        assertEquals(1, GlobalLibraries.get().getLibraries().size());
        final LibraryConfiguration library = GlobalLibraries.get().getLibraries().get(0);
        assertEquals("awesome-lib", library.getName());

        final Mailer.DescriptorImpl descriptor = (Mailer.DescriptorImpl) jenkins.getDescriptor(Mailer.class);
        assertEquals("4441", descriptor.getSmtpPort());
        assertEquals("do-not-reply@acme.org", descriptor.getReplyToAddress());
        assertEquals("smtp.acme.org", descriptor.getSmtpHost() );

        final ArtifactoryBuilder.DescriptorImpl artifactory = (ArtifactoryBuilder.DescriptorImpl) jenkins.getDescriptor(ArtifactoryBuilder.class);
        assertTrue(artifactory.getUseCredentialsPlugin());

        final List<ArtifactoryServer> actifactoryServers = artifactory.getArtifactoryServers();
        assertThat(actifactoryServers, hasSize(1));
        assertThat(actifactoryServers.get(0).getName(), is(equalTo("artifactory")));
        assertThat(actifactoryServers.get(0).getUrl(), is(equalTo("http://acme.com/artifactory")));
        assertThat(actifactoryServers.get(0).getResolverCredentialsConfig().getUsername(), is(equalTo("artifactory_user")));
        assertThat(actifactoryServers.get(0).getResolverCredentialsConfig().getPassword(), is(equalTo("password123")));
    }

}
