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
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;

public class PermissionsTest {

    private static final String RELATIVE_PATH_MANAGE_PAGE = "manage";
    private static final String RELATIVE_PATH_CASC_PAGE = "configuration-as-code";

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void checkPermissionsForReader() throws Exception {
        final String READER = "reader";
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
            .grant(Jenkins.READ).everywhere().to(READER)
        );

        JenkinsRule.WebClient webClient = j.createWebClient()
            .withThrowExceptionOnFailingStatusCode(false);

        webClient.login(READER);
        assertCannotAccessPage(webClient, RELATIVE_PATH_CASC_PAGE);
        assertCannotAccessPage(webClient, RELATIVE_PATH_MANAGE_PAGE);
    }

    @Test
    public void checkPermissionsForSystemReader() throws Exception {
        final String SYSTEM_READER = "systemReader";
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
            .grant(Jenkins.READ).everywhere().to(SYSTEM_READER)
            .grant(Jenkins.SYSTEM_READ).everywhere().to(SYSTEM_READER)
        );

        JenkinsRule.WebClient webClient = j.createWebClient()
            .withThrowExceptionOnFailingStatusCode(false);

        assertUserPermissions(
            webClient,
            SYSTEM_READER,
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
            .grant(Jenkins.MANAGE).everywhere().to(MANAGER)
        );

        JenkinsRule.WebClient webClient = j.createWebClient()
            .withThrowExceptionOnFailingStatusCode(false);

        assertUserPermissions(
            webClient,
            MANAGER,
            ImmutableMap.<Action, Boolean>builder()
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

        HtmlPage cascPage = assertCanAccessPage(webClient, RELATIVE_PATH_CASC_PAGE);
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

    private void assertCannotAccessPage(WebClient webClient, String relativePath) throws Exception {
        final HtmlPage page = webClient.goTo(relativePath);
        final int statusCode = page.getWebResponse().getStatusCode();
        assertThat(
            format("Page %s should not be accessible", relativePath),
            statusCode,
            is(HTTP_FORBIDDEN)
        );
    }

    private void assertCascTileShows(WebClient webClient) throws Exception {
        HtmlPage managePage = assertCanAccessPage(webClient, RELATIVE_PATH_MANAGE_PAGE);
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
