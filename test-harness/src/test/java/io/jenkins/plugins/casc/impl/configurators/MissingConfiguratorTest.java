package io.jenkins.plugins.casc.impl.configurators;

import hudson.model.User;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;



public class MissingConfiguratorTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @ConfiguredWithCode(value = "MissingConfiguratorTest.yml", expected = IllegalArgumentException.class,
            message = "No hudson.security.AuthorizationStrategy implementation found for globalMatrix")
    @Test
    public void testThrowsSuggestion() {
        //No config check needed, should fail with IllegalArgumentException
        //We're purposely trying to configure a plugin for which there is no configurator
        //admin user should not be created due to IllegalArgumentException
        User user = User.getById("admin", false);
        assertThat(user, is(nullValue()));
    }
}
