package io.jenkins.plugins.casc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import hudson.security.ProjectMatrixAuthorizationStrategy;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import java.util.ArrayList;
import java.util.List;
import jenkins.model.Jenkins;
import org.junit.jupiter.api.Test;

/**
 * Created by mads on 2/22/18.
 */
@WithJenkinsConfiguredWithCode
class ProjectMatrixAuthorizationTest {

    @Test
    @ConfiguredWithCode("ProjectMatrixStrategy.yml")
    void checkCorrectlyConfiguredPermissions(JenkinsConfiguredWithCodeRule j) {
        assertEquals(
                ProjectMatrixAuthorizationStrategy.class,
                Jenkins.get().getAuthorizationStrategy().getClass(),
                "The configured instance must use the Global Matrix Authentication Strategy");
        ProjectMatrixAuthorizationStrategy gms =
                (ProjectMatrixAuthorizationStrategy) Jenkins.get().getAuthorizationStrategy();

        List<String> adminPermission =
                new ArrayList<>(gms.getGrantedPermissions().get(Jenkins.ADMINISTER));
        assertEquals("authenticated", adminPermission.get(0));

        List<String> readPermission =
                new ArrayList<>(gms.getGrantedPermissions().get(Jenkins.READ));
        assertEquals("anonymous", readPermission.get(0));
    }
}
