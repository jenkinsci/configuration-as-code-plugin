package org.jenkinsci.plugins.casc.integrations.rolebasedauth;

import com.michelin.cio.hudson.plugins.rolestrategy.Role;
import com.michelin.cio.hudson.plugins.rolestrategy.RoleBasedAuthorizationStrategy;
import hudson.security.AuthorizationStrategy;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.casc.Configurator;
import org.jenkinsci.plugins.casc.misc.CodeConfiguratorRunner;
import org.jenkinsci.plugins.casc.misc.ConfiguredWithCode;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * @author Oleg Nenashev
 * @since TODO
 */
public class RoleStrategyTest {

    @ClassRule
    public static JenkinsRule j = new JenkinsRule();

    @Rule
    public CodeConfiguratorRunner config = new CodeConfiguratorRunner();

    @Test
    public void shouldReturnCustomConfigurator() {
        Configurator c = Configurator.lookup(RoleBasedAuthorizationStrategy.class);
        assertNotNull("Failed to find configurator for RoleBasedAuthorizationStrategy", c);
        assertEquals("Retrieved wrong configurator", RoleBasedAuthorizationStrategyConfigurator.class, c.getClass());
    }

    @Test
    @Issue("Issue #59")
    public void shouldReturnCustomConfiguratorForBaseType() {
        Configurator c = Configurator.lookupForBaseType(AuthorizationStrategy.class, "roleStrategy");
        assertNotNull("Failed to find configurator for RoleBasedAuthorizationStrategy", c);
        assertEquals("Retrieved wrong configurator", RoleBasedAuthorizationStrategyConfigurator.class, c.getClass());

        Configurator.lookup(RoleBasedAuthorizationStrategy.class);
    }

    @Test
    @Issue("Issue #48")
    @ConfiguredWithCode("RoleStrategy1.yml")
    public void shouldReadRolesCorrectly() throws Exception {
        final Jenkins jenkins = Jenkins.getInstance();

        AuthorizationStrategy s = jenkins.getAuthorizationStrategy();
        assertThat("Authorization Strategy has been read incorrectly",
                s, instanceOf(RoleBasedAuthorizationStrategy.class));
        RoleBasedAuthorizationStrategy rbas = (RoleBasedAuthorizationStrategy) s;

        Map<Role, Set<String>> globalRoles = rbas.getGrantedRoles(RoleBasedAuthorizationStrategy.GLOBAL);
        assertThat(globalRoles.size(), equalTo(2));

        //TODO: add more checks to the test
    }
}
