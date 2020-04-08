package io.jenkins.plugins.casc;

import hudson.security.LDAPSecurityRealm;
import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import jenkins.model.IdStrategy;
import jenkins.model.Jenkins;
import jenkins.security.plugins.ldap.FromGroupSearchLDAPGroupMembershipStrategy;
import jenkins.security.plugins.ldap.LDAPConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.RuleChain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author v1v (Victor Martinez)
 */
public class LDAPTest {

    @Rule
    public RuleChain chain = RuleChain.outerRule(new EnvironmentVariables()
            .set("LDAP_PASSWORD", "SECRET"))
            .around(new JenkinsConfiguredWithReadmeRule());

    @Test
    @ConfiguredWithReadme("ldap/README.md")
    public void configure_ldap() {
        final Jenkins jenkins = Jenkins.get();
        final LDAPSecurityRealm securityRealm = (LDAPSecurityRealm) jenkins.getSecurityRealm();
        assertEquals(1, securityRealm.getConfigurations().size());
        assertTrue(securityRealm.getUserIdStrategy() instanceof IdStrategy.CaseInsensitive);
        assertTrue(securityRealm.getGroupIdStrategy() instanceof IdStrategy.CaseSensitive);
        final LDAPConfiguration configuration = securityRealm.getConfigurations().get(0);
        assertEquals("ldap.acme.com", configuration.getServer());
        assertEquals("SECRET", configuration.getManagerPassword());
        assertEquals("manager", configuration.getManagerDN());
        assertEquals("(&(objectCategory=User)(sAMAccountName={0}))", configuration.getUserSearch());
        assertEquals("(&(cn={0})(objectclass=group))", configuration.getGroupSearchFilter());
        final FromGroupSearchLDAPGroupMembershipStrategy strategy = ((FromGroupSearchLDAPGroupMembershipStrategy) configuration.getGroupMembershipStrategy());
        assertEquals("(&(objectClass=group)(|(cn=GROUP_1)(cn=GROUP_2)))", strategy.getFilter());
    }
}
