package io.jenkins.plugins.casc;

import hudson.security.LDAPSecurityRealm;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.model.CNode;
import jenkins.model.IdStrategy;
import jenkins.model.Jenkins;
import jenkins.security.plugins.ldap.LDAPConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.RuleChain;

import static io.jenkins.plugins.casc.misc.Util.getJenkinsRoot;
import static io.jenkins.plugins.casc.misc.Util.toStringFromYamlFile;
import static io.jenkins.plugins.casc.misc.Util.toYamlString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class LDAPSecurityRealmTest {

    @Rule
    public RuleChain chain = RuleChain.outerRule(new EnvironmentVariables()
            .set("LDAP_PASSWORD", "SECRET"))
            .around(new JenkinsConfiguredWithCodeRule());

    @Test
    @ConfiguredWithCode("LDAPSecurityRealmTest.yml")
    public void configure_securityRealm() {
        final Jenkins jenkins = Jenkins.get();
        final LDAPSecurityRealm securityRealm = (LDAPSecurityRealm) jenkins.getSecurityRealm();
        assertEquals(1, securityRealm.getConfigurations().size());
        assertTrue(securityRealm.getUserIdStrategy() instanceof IdStrategy.CaseInsensitive);
        assertTrue(securityRealm.getGroupIdStrategy() instanceof IdStrategy.CaseSensitive);
        final LDAPConfiguration configuration = securityRealm.getConfigurations().get(0);
        assertEquals("ldap.acme.com", configuration.getServer());
        assertEquals("SECRET", configuration.getManagerPassword());
    }

    @Test
    @ConfiguredWithCode("LDAPSecurityRealmTestNoSecret.yml")
    public void export_ldap_no_secret() throws Exception {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        CNode yourAttribute = getJenkinsRoot(context).get("securityRealm").asMapping().get("ldap");

        String exported = toYamlString(yourAttribute);

        String expected = toStringFromYamlFile(this, "LDAPSecurityRealmTestNoSecretExpected.yml");

        assertThat(exported, is(expected));
    }
}
