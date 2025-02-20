package io.jenkins.plugins.casc.impl.configurators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assume.assumeThat;

import hudson.model.User;
import io.jenkins.plugins.casc.UnknownAttributesException;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import org.junit.jupiter.api.Test;

@WithJenkinsConfiguredWithCode
class MissingConfiguratorTest {

    @ConfiguredWithCode(
            value = "MissingConfiguratorTest.yml",
            expected = UnknownAttributesException.class,
            message = "No hudson.security.AuthorizationStrategy implementation found for globalMatrix")
    @Test
    void testThrowsSuggestion(JenkinsConfiguredWithCodeRule j) {
        // The conditions for this test can be false in PCT runs
        assumeThat(j.jenkins.getPlugin("matrix-auth"), nullValue());
        // No config check needed, should fail with IllegalArgumentException
        // We're purposely trying to configure a plugin for which there is no configurator
        // admin user should not be created due to IllegalArgumentException
        User user = User.getById("admin", false);
        assertThat(user, is(nullValue()));
    }
}
