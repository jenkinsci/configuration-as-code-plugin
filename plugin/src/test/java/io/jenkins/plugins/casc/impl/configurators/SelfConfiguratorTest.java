package io.jenkins.plugins.casc.impl.configurators;

import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import jenkins.model.Jenkins;
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
