package io.jenkins.plugins.casc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.jenkins.plugins.casc.misc.Env;
import io.jenkins.plugins.casc.misc.EnvVarsRule;
import io.jenkins.plugins.casc.misc.Envs;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.rules.RuleChain;
import org.jvnet.hudson.test.LoggerRule;
import org.kohsuke.stapler.RequestImpl;
import org.kohsuke.stapler.ResponseImpl;

public class TokenReloadActionTest {

    private Date lastTimeLoaded;

    private TokenReloadAction tokenReloadAction;

    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    @Rule
    public final LoggerRule loggerRule = new LoggerRule();

    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    public EnvVarsRule environment = new EnvVarsRule();

    @Rule
    public RuleChain chain = RuleChain.outerRule(environment).around(j);

    private HttpServletResponse response;

    private RequestImpl newRequest(String authorization) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put(TokenReloadAction.RELOAD_TOKEN_QUERY_PARAMETER, authorization);
        return new RequestImpl(null, new MockHttpServletRequest(parameters), Collections.emptyList(), null);
    }

    private boolean configWasReloaded() {
        return !lastTimeLoaded.equals(ConfigurationAsCode.get().getLastTimeLoaded());
    }

    private void assertConfigReloaded() {
        assertEquals(200, response.getStatus());

        assertTrue(configWasReloaded());

        List<LogRecord> messages = loggerRule.getRecords();
        assertEquals(1, messages.size());
        assertEquals("Configuration reload triggered via token", messages.get(0).getMessage());
        assertEquals(Level.INFO, messages.get(0).getLevel());
    }

    private void assertConfigNotReloadedInvalidToken() {
        assertEquals(401, response.getStatus());

        assertFalse(configWasReloaded());

        List<LogRecord> messages = loggerRule.getRecords();
        assertEquals(1, messages.size());
        assertEquals(
                "Invalid token received, not reloading configuration",
                messages.get(0).getMessage());
        assertEquals(Level.WARNING, messages.get(0).getLevel());
    }

    @Before
    public void setUp() {
        tokenReloadAction = new TokenReloadAction();
        response = new MockHttpServletResponse();
        loggerRule.record(TokenReloadAction.class, Level.ALL);
        loggerRule.capture(3);
        lastTimeLoaded = ConfigurationAsCode.get().getLastTimeLoaded();
    }

    @Test
    public void reloadIsDisabledByDefault() throws IOException {
        System.clearProperty("casc.reload.token");

        RequestImpl request = newRequest(null);
        tokenReloadAction.doIndex(request, new ResponseImpl(null, response));

        assertEquals(404, response.getStatus());

        List<LogRecord> messages = loggerRule.getRecords();
        assertEquals(1, messages.size());
        assertEquals(
                "Configuration reload via token is not enabled", messages.get(0).getMessage());
        assertEquals(Level.WARNING, messages.get(0).getLevel());
        assertFalse(configWasReloaded());
    }

    @Test
    public void reloadReturnsUnauthorizedIfTokenDoesNotMatch() throws IOException {
        System.setProperty("casc.reload.token", "someSecretValue");

        RequestImpl request = newRequest(null);
        tokenReloadAction.doIndex(request, new ResponseImpl(null, response));

        assertConfigNotReloadedInvalidToken();
    }

    @Test
    public void reloadReturnsOkWhenCalledWithValidToken() throws IOException {
        System.setProperty("casc.reload.token", "someSecretValue");

        tokenReloadAction.doIndex(newRequest("someSecretValue"), new ResponseImpl(null, response));

        assertConfigReloaded();
    }

    @Test
    @Envs({@Env(name = "CASC_RELOAD_TOKEN", value = "someSecretValue")})
    public void reloadReturnsOkWhenCalledWithValidTokenSetByEnvVar() throws IOException {
        tokenReloadAction.doIndex(newRequest("someSecretValue"), new ResponseImpl(null, response));

        assertConfigReloaded();
    }

    @Test
    @Envs({@Env(name = "CASC_RELOAD_TOKEN", value = "someSecretValue")})
    public void reloadShouldNotUseTokenFromPropertyIfEnvVarIsSet() throws IOException {
        System.setProperty("casc.reload.token", "otherSecretValue");

        tokenReloadAction.doIndex(newRequest("otherSecretValue"), new ResponseImpl(null, response));

        assertConfigNotReloadedInvalidToken();
    }

    @Test
    @Envs({@Env(name = "CASC_RELOAD_TOKEN", value = "")})
    public void reloadShouldUsePropertyAsTokenIfEnvVarIsEmpty() throws IOException {
        System.setProperty("casc.reload.token", "someSecretValue");

        tokenReloadAction.doIndex(newRequest("someSecretValue"), new ResponseImpl(null, response));

        assertConfigReloaded();
    }

    @Test
    public void displayName() {
        assertEquals("Reload Configuration as Code", tokenReloadAction.getDisplayName());
    }
}
