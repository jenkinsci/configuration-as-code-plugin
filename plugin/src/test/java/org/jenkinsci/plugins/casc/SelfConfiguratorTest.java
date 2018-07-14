package org.jenkinsci.plugins.casc;

import jenkins.model.Jenkins;
import org.jenkinsci.plugins.casc.misc.ConfiguredWithCode;
import org.jenkinsci.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.jenkinsci.plugins.casc.settings.Deprecation;
import org.jenkinsci.plugins.casc.settings.Restriction;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class SelfConfiguratorTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode(value = "SelfConfiguratorTest.yml")
    public void self_configure() {
        assertEquals(Deprecation.warn, ConfigurationAsCode.get().getDeprecation());
        assertEquals(Restriction.warn, ConfigurationAsCode.get().getRestricted());
        assertEquals("/tmp", Jenkins.getInstance().getRawBuildsDir());
    }

    @Test
    @ConfiguredWithCode(value = "SelfConfiguratorRestrictedTest.yml", expected = ConfiguratorException.class)
    public void self_configure_restricted() {
    }
}
