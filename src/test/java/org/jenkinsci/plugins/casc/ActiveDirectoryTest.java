package org.jenkinsci.plugins.casc;

import hudson.plugins.active_directory.ActiveDirectoryDomain;
import hudson.plugins.active_directory.ActiveDirectorySecurityRealm;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class ActiveDirectoryTest {


    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void configure_active_directory() throws Exception {
        ConfigurationAsCode.configure(getClass().getResourceAsStream("ActiveDirectoryTest.yml"));

        final Jenkins jenkins = Jenkins.getInstance();
        final ActiveDirectorySecurityRealm realm = (ActiveDirectorySecurityRealm) jenkins.getSecurityRealm();
        assertEquals(1, realm.domains.size());
        final ActiveDirectoryDomain domain = realm.domains.get(0);
        assertEquals("acme", domain.name);
        assertEquals("admin", domain.bindName);
        assertEquals("ADMIN123", domain.bindPassword.getPlainText());
        assertEquals("ad1.acme.com:123,ad2.acme.com:456", domain.servers);

        assertTrue(realm.removeIrrelevantGroups);
        assertTrue(realm.startTls);
    }
}
