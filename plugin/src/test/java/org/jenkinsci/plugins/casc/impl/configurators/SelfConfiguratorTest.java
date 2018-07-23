package org.jenkinsci.plugins.casc.impl.configurators;

import jenkins.model.Jenkins;
import org.jenkinsci.plugins.casc.ConfigurationAsCode;
import org.jenkinsci.plugins.casc.ConfigurationContext;
import org.jenkinsci.plugins.casc.ConfiguratorException;
import org.jenkinsci.plugins.casc.misc.ConfiguredWithCode;
import org.jenkinsci.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
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
        assertEquals("/tmp", Jenkins.getInstance().getRawBuildsDir());
    }

    @Test
    @ConfiguredWithCode(value = "SelfConfiguratorRestrictedTest.yml", expected = ConfiguratorException.class)
    public void self_configure_restricted() {
    }
}
