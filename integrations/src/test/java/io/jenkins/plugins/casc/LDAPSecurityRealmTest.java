package io.jenkins.plugins.casc;

import hudson.security.LDAPSecurityRealm;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.EnvVarsRule;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import jenkins.model.IdStrategy;
import jenkins.model.Jenkins;
import jenkins.security.plugins.ldap.LDAPConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class LDAPSecurityRealmTest {

    @Rule
    public RuleChain chain = RuleChain.outerRule(new EnvVarsRule()
            .env("LDAP_PASSWORD", "SECRET"))
            .around(new JenkinsConfiguredWithCodeRule());

    @Test
    @ConfiguredWithCode("LDAPSecurityRealmTest.yml")
    public void configure_securityRealm() {
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
