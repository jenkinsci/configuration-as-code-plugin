package io.jenkins.plugins.casc;

import hudson.security.HudsonPrivateSecurityRealm;
import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:VictorMartinezRubio@gmail.com">Victor Martinez</a>
 */
public class EmbeddedUserDatabaseTest {

    @Rule
    public JenkinsConfiguredWithReadmeRule j = new JenkinsConfiguredWithReadmeRule();

    @Test
    @ConfiguredWithReadme("embedded-userdatabase/README.md")
    public void configure_user_database() throws Exception {
        final Jenkins jenkins = Jenkins.get();
        final HudsonPrivateSecurityRealm realm = (HudsonPrivateSecurityRealm) jenkins.getSecurityRealm();
        assertTrue(jenkins.getAuthorizationStrategy().getACL(realm.getUser("admin")).hasPermission(Jenkins.ADMINISTER));
        assertFalse(realm.getAllowsSignup());
        assertThat(realm.getAllUsers(), hasSize(1));
    }
}
