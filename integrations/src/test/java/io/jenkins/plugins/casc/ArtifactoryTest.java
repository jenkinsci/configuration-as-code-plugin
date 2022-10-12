package io.jenkins.plugins.casc;

import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import java.util.List;
import jenkins.model.Jenkins;
import org.jfrog.hudson.ArtifactoryBuilder;
import org.jfrog.hudson.JFrogPlatformInstance;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.RuleChain;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertTrue;

public class ArtifactoryTest {

    @Rule
    public RuleChain chain = RuleChain.outerRule(new EnvironmentVariables()
        .set("ARTIFACTORY_PASSWORD", "password123"))
        .around(new JenkinsConfiguredWithReadmeRule());

    @Test
    @ConfiguredWithReadme(value = "artifactory/README.md")
    public void configure_artifactory() {
        final Jenkins jenkins = Jenkins.get();
        final ArtifactoryBuilder.DescriptorImpl descriptor = (ArtifactoryBuilder.DescriptorImpl) jenkins.getDescriptor(ArtifactoryBuilder.class);
        assertTrue(descriptor.getUseCredentialsPlugin());

        final List<JFrogPlatformInstance> jfrogInstances = descriptor.getJfrogInstances();
        assertThat(jfrogInstances, hasSize(1));
        assertThat(jfrogInstances.get(0).getId(), is(equalTo("artifactory")));
        assertThat(jfrogInstances.get(0).getPlatformUrl(), is(equalTo("http://acme.com/artifactory")));
        assertThat(jfrogInstances.get(0).getArtifactoryUrl(), is(equalTo("http://acme.com/artifactory")));
        assertThat(jfrogInstances.get(0).getDistributionUrl(), is(equalTo("http://acme.com/distribution")));
        assertThat(jfrogInstances.get(0).getDeployerCredentialsConfig().getCredentialsId(), is(equalTo("artifactory")));
        assertThat(jfrogInstances.get(0).getResolverCredentialsConfig().getUsername(), is(equalTo("artifactory_user")));
        assertThat(jfrogInstances.get(0).getResolverCredentialsConfig().getPassword().getPlainText(), is(equalTo("password123")));

    }
}
