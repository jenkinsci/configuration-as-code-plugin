package org.jenkinsci.plugins.casc;

import hudson.plugins.active_directory.ActiveDirectoryDomain;
import hudson.plugins.active_directory.ActiveDirectorySecurityRealm;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.casc.misc.ConfiguredWithCode;
import org.jenkinsci.plugins.casc.misc.EnvVarsRule;
import org.jenkinsci.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class SelfConfiguratorTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode(value = "SelfConfiguratorTest.yml")
    public void self_configure() {
        assertEquals(ConfigurationAsCode.Version.ONE, ConfigurationAsCode.get().version);
        assertEquals(ConfigurationAsCode.Deprecation.warn, ConfigurationAsCode.get().getDeprecation());
        assertEquals(ConfigurationAsCode.Restricted.beta, ConfigurationAsCode.get().getRestricted());
    }
}
