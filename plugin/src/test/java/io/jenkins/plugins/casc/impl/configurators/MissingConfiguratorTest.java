package io.jenkins.plugins.casc.impl.configurators;

import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.Rule;
import org.junit.Test;

public class MissingConfiguratorTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @ConfiguredWithCode(value = "MissingConfiguratorTest.yml", expected = IllegalArgumentException.class,
            message = "No hudson.security.AuthorizationStrategy implementation found for globalMatrix")
    @Test
    public void testThrowsSuggestion() throws Exception {
        //No config check needed, should fail with IllegalArgumentException
        //We're purposely trying to configure a plugin for which there is no configurator
    }
}
