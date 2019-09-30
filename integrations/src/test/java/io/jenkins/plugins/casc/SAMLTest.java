package io.jenkins.plugins.casc;

import hudson.security.SecurityRealm;
import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import org.jenkinsci.plugins.saml.SamlSecurityRealm;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

/**
 * @author v1v (Victor Martinez)
 */
public class SAMLTest {

    @Rule
    public JenkinsConfiguredWithReadmeRule j = new JenkinsConfiguredWithReadmeRule();

    @Test
    @ConfiguredWithReadme("saml/README.md")
    public void configure_saml_security() throws Exception {
        SecurityRealm realm = j.jenkins.get().getSecurityRealm();
        assertThat(realm, instanceOf(SamlSecurityRealm.class));
        SamlSecurityRealm securityRealm = (SamlSecurityRealm)realm;

        assertThat(securityRealm.getBinding(), is("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect"));
        assertThat(securityRealm.getDisplayNameAttributeName(), is("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name"));
        assertThat(securityRealm.getEmailAttributeName(), is("Email"));
        assertThat(securityRealm.getGroupsAttributeName(), is("http://schemas.xmlsoap.org/claims/Group"));
        assertThat(securityRealm.getMaximumAuthenticationLifetime(), is(86400));
        assertThat(securityRealm.getUsernameAttributeName(), is("NameID"));
        assertThat(securityRealm.getUsernameCaseConversion(), is("none"));
    }
}
