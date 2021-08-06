package io.jenkins.plugins.casc.permissions;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.jvnet.hudson.test.MockAuthorizationStrategy;

import static io.jenkins.plugins.casc.permissions.Action.APPLY_NEW_CONFIGURATION;
import static io.jenkins.plugins.casc.permissions.Action.DOWNLOAD_CONFIGURATION;
import static io.jenkins.plugins.casc.permissions.Action.RELOAD_EXISTING_CONFIGURATION;
import static io.jenkins.plugins.casc.permissions.Action.VIEW_CONFIGURATION;
import static java.lang.String.format;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;

public class PermissionsTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void checkPermissionsForSimpleUser() throws Exception {
        final String USER = "user";
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
            .grant(Jenkins.READ).everywhere().to(USER)
            .grant(Jenkins.SYSTEM_READ).everywhere().to(USER)
        );

        JenkinsRule.WebClient webClient = j.createWebClient()
            .withThrowExceptionOnFailingStatusCode(false);

        assertUserPermissions(
            webClient,
            USER,
            ImmutableMap.<Action, Boolean>builder()
                .put(VIEW_CONFIGURATION, true)
                .put(DOWNLOAD_CONFIGURATION, true)
                .put(APPLY_NEW_CONFIGURATION, false)
                .put(RELOAD_EXISTING_CONFIGURATION, false)
                .build()
        );
    }

    @Test
    public void checkPermissionsForManager() throws Exception {
        final String MANAGER = "manager";
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
            .grant(Jenkins.READ).everywhere().to(MANAGER)
            .grant(Jenkins.SYSTEM_READ).everywhere().to(MANAGER)
            .grant(Jenkins.MANAGE).everywhere().to(MANAGER)
        );

        JenkinsRule.WebClient webClient = j.createWebClient()
            .withThrowExceptionOnFailingStatusCode(false);

        assertUserPermissions(
            webClient,
            MANAGER,
            ImmutableMap.<Action, Boolean>builder()
                .put(VIEW_CONFIGURATION, true)
                .put(DOWNLOAD_CONFIGURATION, true)
                .put(APPLY_NEW_CONFIGURATION, false)
                .put(RELOAD_EXISTING_CONFIGURATION, true)
                .build()
        );
    }

    @Test
    public void checkPermissionsForAdmin() throws Exception {
        final String ADMIN = "admin";
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
            .grant(Jenkins.ADMINISTER).everywhere().to(ADMIN)
        );

        JenkinsRule.WebClient webClient = j.createWebClient()
            .withThrowExceptionOnFailingStatusCode(false);

        assertUserPermissions(
            webClient,
            ADMIN,
            ImmutableMap.<Action, Boolean>builder()
                .put(VIEW_CONFIGURATION, true)
                .put(DOWNLOAD_CONFIGURATION, true)
                .put(APPLY_NEW_CONFIGURATION, true)
                .put(RELOAD_EXISTING_CONFIGURATION, true)
                .build()
        );
    }

    private void assertUserPermissions(WebClient webClient, String user,
        Map<Action, Boolean> allowedActions) throws Exception {

        webClient.login(user);
        assertCascTileShows(webClient);

        HtmlPage cascPage = assertCanAccessPage(webClient, "configuration-as-code");
        allowedActions.forEach(
            (action, isAllowed) -> assertActionAvailable(cascPage, action, isAllowed)
        );
    }

    private HtmlPage assertCanAccessPage(WebClient webClient, String relativePath)
        throws Exception {
        HtmlPage page = webClient.goTo(relativePath);
        assertEquals(HTTP_OK, page.getWebResponse().getStatusCode());
        return page;
    }

    private void assertCascTileShows(WebClient webClient) throws Exception {
        HtmlPage managePage = assertCanAccessPage(webClient, "manage");
        final String pageContent = managePage.getWebResponse().getContentAsString();
        assertThat(
            "The user should have access to the CasC tile in management page",
            pageContent,
            containsString("Configuration as Code")
        );
    }

    private void assertActionAvailable(HtmlPage page, Action action, boolean shouldContain) {
        String responseContent = page.getWebResponse().getContentAsString();
        if (shouldContain) {
            assertThat(
                format("Action %s should be available", action.name()),
                responseContent,
                containsString(action.buttonText)
            );
        } else {
            assertThat(
                format("Action %s should not be available", action.name()),
                responseContent,
                not(containsString(action.buttonText))
            );
        }
    }
}
