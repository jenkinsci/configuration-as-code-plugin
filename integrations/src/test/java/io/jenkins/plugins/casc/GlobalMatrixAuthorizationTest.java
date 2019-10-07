package io.jenkins.plugins.casc;

import hudson.security.GlobalMatrixAuthorizationStrategy;
import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import java.util.ArrayList;
import java.util.List;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Mads Nielsen
 * @since 1.0
 */
public class GlobalMatrixAuthorizationTest {

    @Rule
    public JenkinsConfiguredWithReadmeRule j = new JenkinsConfiguredWithReadmeRule();

    @Test
    @ConfiguredWithReadme("matrix-auth/README.md")
    public void checkCorrectlyConfiguredPermissions() throws Exception {
        assertEquals("The configured instance must use the Global Matrix Authentication Strategy", GlobalMatrixAuthorizationStrategy.class, Jenkins.get().getAuthorizationStrategy().getClass());
        GlobalMatrixAuthorizationStrategy gms = (GlobalMatrixAuthorizationStrategy) Jenkins.get().getAuthorizationStrategy();

        List<String> adminPermission = new ArrayList<>(gms.getGrantedPermissions().get(Jenkins.ADMINISTER));
        assertEquals("authenticated", adminPermission.get(0));

        List<String> readPermission = new ArrayList<>(gms.getGrantedPermissions().get(Jenkins.READ));
        assertEquals("anonymous", readPermission.get(0));
    }
}
