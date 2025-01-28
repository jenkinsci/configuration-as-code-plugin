package io.jenkins.plugins.casc.impl.configurators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@WithJenkinsConfiguredWithCode
class SelfConfiguratorTest {

    @Test
    @ConfiguredWithCode(value = "SelfConfiguratorTest.yml")
    void self_configure(JenkinsConfiguredWithCodeRule j) {
        assertThat(j.jenkins.getRawBuildsDir(), is("/tmp"));
    }

    @Test
    @ConfiguredWithCode(value = "SelfConfiguratorRestrictedTest.yml", expected = ConfiguratorException.class)
    void self_configure_restricted(JenkinsConfiguredWithCodeRule j) {
        // expected to throw Configurator Exception
        assertThat(j.jenkins.getRawBuildsDir(), is(not("/tmp")));
    }
}
