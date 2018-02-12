package org.jenkinsci.plugins.casc.integrations.globalmatrixauth;

import hudson.security.AuthorizationStrategy;
import hudson.security.GlobalMatrixAuthorizationStrategy;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.casc.ConfigurationAsCode;
import org.jenkinsci.plugins.casc.Configurator;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Mads Nielsen
 * @since TODO
 */
public class GlobalMatrixAuthorizationTest {

    @ClassRule
    public static JenkinsRule j = new JenkinsRule();

    @Test
    public void shouldReturnCustomConfigurator() {
        Configurator configurator = Configurator.lookup(GlobalMatrixAuthorizationStrategy.class);
        assertNotNull("Failed to find configurator for GlobalMatrixAuthorizationStrategy", configurator);
        assertEquals("Retrieved wrong configurator", GlobalMatrixAuthorizationStrategyConfigurator.class, configurator.getClass());
    }

    @Test
    public void shouldReturnCustomConfiguratorForBaseType() {
        Configurator c = Configurator.lookupForBaseType(AuthorizationStrategy.class, "globalMatrix");
        assertNotNull("Failed to find configurator for GlobalMatrixAuthorizationStrategy", c);
        assertEquals("Retrieved wrong configurator", GlobalMatrixAuthorizationStrategyConfigurator.class, c.getClass());
        Configurator.lookup(GlobalMatrixAuthorizationStrategy.class);
    }

    @Test
    public void checkCorrectlyConfiguredPermissions() throws Exception {
        ConfigurationAsCode.configure(getClass().getResourceAsStream("GlobalMatrixStrategy.yml"));
        assertEquals("The configured instance must use the Global Matrix Authentication Strategy", GlobalMatrixAuthorizationStrategy.class, Jenkins.getInstance().getAuthorizationStrategy().getClass());
        GlobalMatrixAuthorizationStrategy gms = (GlobalMatrixAuthorizationStrategy)Jenkins.getInstance().getAuthorizationStrategy();

        List<String> adminPermission = new ArrayList<>(gms.getGrantedPermissions().get(Jenkins.ADMINISTER));
        assertEquals("authenticated", adminPermission.get(0));

        List<String> readPermission = new ArrayList<>(gms.getGrantedPermissions().get(Jenkins.READ));
        assertEquals("anonymous", readPermission.get(0));
    }
}
