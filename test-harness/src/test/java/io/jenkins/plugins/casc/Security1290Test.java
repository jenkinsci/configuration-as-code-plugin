package io.jenkins.plugins.casc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import jenkins.model.Jenkins;
import org.htmlunit.HttpMethod;
import org.htmlunit.WebRequest;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;

@WithJenkinsConfiguredWithCode
class Security1290Test {

    @Test
    void configurationAsCodePagesPermissions(JenkinsConfiguredWithCodeRule j) throws Exception {
        final String ADMIN = "admin";
        final String USER = "user";

        j.jenkins.setCrumbIssuer(null);
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Jenkins.ADMINISTER)
                .everywhere()
                .to(ADMIN)
                .grant(Jenkins.READ)
                .everywhere()
                .to(USER));

        JenkinsRule.WebClient adminWc = j.createWebClient();
        adminWc.login(ADMIN);

        JenkinsRule.WebClient userWc = j.createWebClient().withThrowExceptionOnFailingStatusCode(false);
        userWc.login(USER);

        assertRightPermissionConfigurations(j, "configuration-as-code/schema", adminWc, userWc);
        assertRightPermissionConfigurations(j, "configuration-as-code/reference", adminWc, userWc);
    }

    private void assertRightPermissionConfigurations(
            JenkinsConfiguredWithCodeRule j,
            String relativeUrl,
            JenkinsRule.WebClient adminWc,
            JenkinsRule.WebClient userWc)
            throws IOException {
        WebRequest request = new WebRequest(new URL(j.getURL() + relativeUrl), HttpMethod.GET);

        assertEquals(
                HttpURLConnection.HTTP_OK,
                adminWc.getPage(request).getWebResponse().getStatusCode());
        assertEquals(
                HttpURLConnection.HTTP_FORBIDDEN,
                userWc.getPage(request).getWebResponse().getStatusCode());
    }
}
