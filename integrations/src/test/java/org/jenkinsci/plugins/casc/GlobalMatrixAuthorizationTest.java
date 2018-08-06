package org.jenkinsci.plugins.casc;

import hudson.security.AuthorizationStrategy;
import hudson.security.GlobalMatrixAuthorizationStrategy;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.casc.impl.DefaultConfiguratorRegistry;
import org.jenkinsci.plugins.casc.integrations.globalmatrixauth.GlobalMatrixAuthorizationStrategyConfigurator;
import org.jenkinsci.plugins.casc.misc.ConfiguredWithCode;
import org.jenkinsci.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Mads Nielsen
 * @since TODO
 */
public class GlobalMatrixAuthorizationTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();


    @Test
    public void shouldReturnCustomConfigurator() {
        Configurator configurator = ConfiguratorRegistry.get().lookup(GlobalMatrixAuthorizationStrategy.class);
        assertNotNull("Failed to find configurator for GlobalMatrixAuthorizationStrategy", configurator);
        assertEquals("Retrieved wrong configurator", GlobalMatrixAuthorizationStrategyConfigurator.class, configurator.getClass());
    }
    
    @Test
    @ConfiguredWithCode("GlobalMatrixStrategy.yml")
    public void checkCorrectlyConfiguredPermissions() throws Exception {
        assertEquals("The configured instance must use the Global Matrix Authentication Strategy", GlobalMatrixAuthorizationStrategy.class, Jenkins.getInstance().getAuthorizationStrategy().getClass());
        GlobalMatrixAuthorizationStrategy gms = (GlobalMatrixAuthorizationStrategy) Jenkins.getInstance().getAuthorizationStrategy();

        List<String> adminPermission = new ArrayList<>(gms.getGrantedPermissions().get(Jenkins.ADMINISTER));
        assertEquals("authenticated", adminPermission.get(0));

        List<String> readPermission = new ArrayList<>(gms.getGrantedPermissions().get(Jenkins.READ));
        assertEquals("anonymous", readPermission.get(0));
    }
}
