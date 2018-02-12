package org.jenkinsci.plugins.casc;

import hudson.plugins.active_directory.ActiveDirectoryDomain;
import hudson.plugins.active_directory.ActiveDirectorySecurityRealm;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.casc.misc.CodeConfiguratorRunner;
import org.jenkinsci.plugins.casc.misc.ConfiguredWithCode;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.rules.RuleChain;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class ActiveDirectoryTest {

    public JenkinsRule j = new JenkinsRule();
    public CodeConfiguratorRunner config = new CodeConfiguratorRunner();

    @Rule
    public RuleChain chain = RuleChain.outerRule(j)
            .around(new ExternalResource() {
                @Override
                protected void before() {
                    System.setProperty("BIND_PASSWORD", "ADMIN123");
                }
            })
            .around(config);

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

        assertTrue(realm.removeIrrelevantGroups);
        assertTrue(realm.startTls);
    }
}
