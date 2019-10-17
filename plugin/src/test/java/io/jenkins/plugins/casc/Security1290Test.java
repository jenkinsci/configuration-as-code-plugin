package io.jenkins.plugins.casc;

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebRequest;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;

import static org.junit.Assert.assertEquals;

public class Security1290Test {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    public void configurationAsCodePagesPermissions() throws Exception {
        final String ADMIN = "admin";
        final String USER = "user";

        j.jenkins.setCrumbIssuer(null);
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Jenkins.ADMINISTER).everywhere().to(ADMIN)
                .grant(Jenkins.READ).everywhere().to(USER)
        );

        JenkinsRule.WebClient adminWc = j.createWebClient();
        adminWc.login(ADMIN);

        JenkinsRule.WebClient userWc = j.createWebClient()
                .withThrowExceptionOnFailingStatusCode(false);
        userWc.login(USER);

        assertRightPermissionConfigurations("configuration-as-code/schema", adminWc, userWc);
        assertRightPermissionConfigurations("configuration-as-code/reference", adminWc, userWc);
    }

    private void assertRightPermissionConfigurations(String relativeUrl, JenkinsRule.WebClient adminWc, JenkinsRule.WebClient userWc) throws IOException {
        WebRequest request = new WebRequest(new URL(j.getURL() + relativeUrl), HttpMethod.GET);

        assertEquals(HttpURLConnection.HTTP_OK, adminWc.getPage(request).getWebResponse().getStatusCode());
        assertEquals(HttpURLConnection.HTTP_FORBIDDEN, userWc.getPage(request).getWebResponse().getStatusCode());
    }
}
