package io.jenkins.plugins.casc;

import hudson.security.SecurityRealm;
import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import org.jenkinsci.plugins.KeycloakSecurityRealm;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

/**
 * @author v1v (Victor Martinez)
 */
public class KeycloakTest {

    @Rule
    public JenkinsConfiguredWithReadmeRule j = new JenkinsConfiguredWithReadmeRule();

    @Test
    @ConfiguredWithReadme("keycloak/README.md")
    public void configure_artifact_manager() throws Exception {
        SecurityRealm realm = j.jenkins.get().getSecurityRealm();
        assertThat(realm, instanceOf(KeycloakSecurityRealm.class));
        KeycloakSecurityRealm securityRealm = (KeycloakSecurityRealm)realm;
        assertThat(securityRealm.getKeycloakJson(), containsString("\"auth-server-url\": \"https://my-keycloak-url/auth\""));
    }
}
