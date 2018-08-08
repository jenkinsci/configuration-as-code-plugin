package io.jenkins.plugins.casc;

import hudson.plugins.active_directory.ActiveDirectoryDomain;
import hudson.plugins.active_directory.ActiveDirectorySecurityRealm;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.EnvVarsRule;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class ActiveDirectoryTest {

    @Rule
    public RuleChain chain = RuleChain.outerRule(new EnvVarsRule()
            .env("BIND_PASSWORD", "ADMIN123"))
            .around(new JenkinsConfiguredWithCodeRule());

    @Test
    @ConfiguredWithCode(value = "ActiveDirectoryTest.yml")
    public void configure_active_directory() throws Exception {
        final Jenkins jenkins = Jenkins.getInstance();
        final ActiveDirectorySecurityRealm realm = (ActiveDirectorySecurityRealm) jenkins.getSecurityRealm();
        assertEquals(1, realm.domains.size());
        final ActiveDirectoryDomain domain = realm.domains.get(0);
        assertEquals("acme", domain.name);
        assertEquals("admin", domain.bindName);
        assertEquals("ADMIN123", domain.bindPassword.getPlainText());
        assertEquals("ad1.acme.com:123,ad2.acme.com:456", domain.servers);
        assertEquals("jenkins", realm.getJenkinsInternalUser());

        assertTrue(realm.removeIrrelevantGroups);
        assertTrue(realm.startTls);
    }
}
