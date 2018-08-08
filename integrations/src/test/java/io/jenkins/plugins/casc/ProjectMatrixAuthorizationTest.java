package io.jenkins.plugins.casc;

import hudson.security.ProjectMatrixAuthorizationStrategy;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by mads on 2/22/18.
 */
public class ProjectMatrixAuthorizationTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("ProjectMatrixStrategy.yml")
    public void checkCorrectlyConfiguredPermissions() throws Exception {
        assertEquals("The configured instance must use the Global Matrix Authentication Strategy", ProjectMatrixAuthorizationStrategy.class, Jenkins.getInstance().getAuthorizationStrategy().getClass());
        ProjectMatrixAuthorizationStrategy gms = (ProjectMatrixAuthorizationStrategy) Jenkins.getInstance().getAuthorizationStrategy();

        List<String> adminPermission = new ArrayList<>(gms.getGrantedPermissions().get(Jenkins.ADMINISTER));
        assertEquals("authenticated", adminPermission.get(0));

        List<String> readPermission = new ArrayList<>(gms.getGrantedPermissions().get(Jenkins.READ));
        assertEquals("anonymous", readPermission.get(0));
    }
}
