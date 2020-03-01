package io.jenkins.plugins.casc;

import hudson.plugins.active_directory.ActiveDirectoryDomain;
import hudson.plugins.active_directory.ActiveDirectorySecurityRealm;
import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.RuleChain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class ActiveDirectoryTest {

    @Rule
    public RuleChain chain = RuleChain.outerRule(new EnvironmentVariables()
            .set("BIND_PASSWORD", "ADMIN123"))
            .around(new JenkinsConfiguredWithReadmeRule());

    @Test
    @ConfiguredWithReadme("active-directory/README.md")
    public void configure_active_directory() throws Exception {
        final Jenkins jenkins = Jenkins.get();
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
        assertNotNull(realm.getCache());
        assertEquals(500, realm.getCache().getSize());
        assertEquals(600, realm.getCache().getTtl());

    }
}
