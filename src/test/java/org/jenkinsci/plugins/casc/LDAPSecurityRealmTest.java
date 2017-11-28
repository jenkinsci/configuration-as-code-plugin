package org.jenkinsci.plugins.casc;

import hudson.security.LDAPSecurityRealm;
import hudson.security.SecurityRealm;
import hudson.tasks.Mailer;
import jenkins.model.IdStrategy;
import jenkins.model.Jenkins;
import jenkins.security.plugins.ldap.LDAPConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class LDAPSecurityRealmTest {


    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void configure_securityRealm() throws Exception {
        System.setProperty("LDAP_PASSWORD", "SECRET");
        ConfigurationAsCode.configure(getClass().getResourceAsStream("LDAPSecurityRealmTest.yml"));

        final Jenkins jenkins = Jenkins.getInstance();
        final LDAPSecurityRealm securityRealm = (LDAPSecurityRealm) jenkins.getSecurityRealm();
        assertEquals(1, securityRealm.getConfigurations().size());
        assertTrue(securityRealm.getUserIdStrategy() instanceof IdStrategy.CaseInsensitive);
        assertTrue(securityRealm.getGroupIdStrategy() instanceof IdStrategy.CaseSensitive);
        final LDAPConfiguration configuration = securityRealm.getConfigurations().get(0);
        assertEquals("ldap.acme.com", configuration.getServer());
        assertEquals("SECRET", configuration.getManagerPassword());
    }
}
