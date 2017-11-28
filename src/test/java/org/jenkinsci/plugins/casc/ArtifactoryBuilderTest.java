package org.jenkinsci.plugins.casc;

import hudson.model.Descriptor;
import hudson.security.LDAPSecurityRealm;
import jenkins.model.IdStrategy;
import jenkins.model.Jenkins;
import jenkins.security.plugins.ldap.LDAPConfiguration;
import org.jfrog.hudson.ArtifactoryBuilder;
import org.jfrog.hudson.ArtifactoryServer;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class ArtifactoryBuilderTest {


    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void configure_artifactory() throws Exception {
        ConfigurationAsCode.configure(getClass().getResourceAsStream("ArtifactoryBuilderTest.yml"));

        final Jenkins jenkins = Jenkins.getInstance();
        final ArtifactoryBuilder.DescriptorImpl descriptor = (ArtifactoryBuilder.DescriptorImpl) jenkins.getDescriptor(ArtifactoryBuilder.class);
        assertTrue(descriptor.getUseCredentialsPlugin());
        assertEquals(1, descriptor.getArtifactoryServers().size());
        final ArtifactoryServer server = descriptor.getArtifactoryServers().get(0);
        assertEquals("artifactory", server.getName());
        assertEquals("http://acme.com/artifactory", server.getUrl());
        assertEquals("artifactory_user", server.getResolverCredentialsConfig().getUsername());
        assertEquals("SECRET", server.getResolverCredentialsConfig().getPassword());

    }
}
