package io.jenkins.plugins.casc;

import static org.junit.Assert.assertEquals;

import hudson.model.Job;
import hudson.security.GlobalMatrixAuthorizationStrategy;
import hudson.security.ProjectMatrixAuthorizationStrategy;
import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import java.util.Set;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.matrixauth.PermissionEntry;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

/**
 * @author Mads Nielsen
 * @since 1.0
 */
public class MatrixAuthorizationTest {

    @Rule
    public JenkinsConfiguredWithReadmeRule j = new JenkinsConfiguredWithReadmeRule();

    @Test
    @ConfiguredWithReadme("matrix-auth/README.md#0")
    public void checkGlobalCorrectlyConfiguredPermissions() {
        assertEquals(
                "The configured instance must use the Global Matrix Authentication Strategy",
                GlobalMatrixAuthorizationStrategy.class,
                Jenkins.get().getAuthorizationStrategy().getClass());
        GlobalMatrixAuthorizationStrategy gms =
                (GlobalMatrixAuthorizationStrategy) Jenkins.get().getAuthorizationStrategy();

        Set<PermissionEntry> adminPermission = gms.getGrantedPermissionEntries()
            .get(Job.BUILD);
        assertEquals("authenticated", adminPermission.iterator().next().getSid());

        Set<PermissionEntry> readPermission = gms.getGrantedPermissionEntries()
            .get(Job.READ);
        assertEquals("anonymous", readPermission.iterator().next().getSid());
    }

    @Test
    @ConfiguredWithReadme("matrix-auth/README.md#1")
    public void checkProjectCorrectlyConfiguredPermissions() {
        Assertions.assertEquals(
            ProjectMatrixAuthorizationStrategy.class,
            Jenkins.get().getAuthorizationStrategy().getClass(),
            "The configured instance must use the Global Matrix Authentication Strategy");
        ProjectMatrixAuthorizationStrategy gms =
            (ProjectMatrixAuthorizationStrategy) Jenkins.get().getAuthorizationStrategy();

        Set<PermissionEntry> adminPermission = gms.getGrantedPermissionEntries()
            .get(Jenkins.ADMINISTER);
        Assertions.assertEquals("authenticated", adminPermission.iterator().next().getSid());

        Set<PermissionEntry> readPermission = gms.getGrantedPermissionEntries()
            .get(Jenkins.READ);
        Assertions.assertEquals("anonymous", readPermission.iterator().next().getSid());
    }

}
