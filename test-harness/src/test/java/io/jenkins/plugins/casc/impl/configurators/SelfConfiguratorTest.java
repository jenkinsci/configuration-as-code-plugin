package io.jenkins.plugins.casc.impl.configurators;

import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;


/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class SelfConfiguratorTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode(value = "SelfConfiguratorTest.yml")
    public void self_configure() {
        assertThat(j.jenkins.getRawBuildsDir(), is("/tmp"));
    }

    @Test
    @ConfiguredWithCode(value = "SelfConfiguratorRestrictedTest.yml", expected = ConfiguratorException.class)
    public void self_configure_restricted() {
        // expected to throw Configurator Exception
        assertThat(j.jenkins.getRawBuildsDir(), is(not("/tmp")));
    }
}
