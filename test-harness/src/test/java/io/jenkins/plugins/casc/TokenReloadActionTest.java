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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
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

    private ServletResponseSpy response;

    private static class ServletResponseSpy extends Response {
        private int error = 200;

        public ServletResponseSpy() {
            super(null, null);
        }

        @Override
        public void sendError(int sc) {
            error = sc;
        }

        @Override
        public int getStatus() {
            return error;
        }
    }

    private static class RequestStub extends Request {
        private final String authorization;

        public RequestStub(String authorization) {
            super(null, null);
            this.authorization = authorization;
        }

        @Override
        public String getParameter(String name) {
            if (TokenReloadAction.RELOAD_TOKEN_QUERY_PARAMETER.equals(name)) {
                return authorization;
            }
            return super.getHeader(name);
        }
    }

    private RequestImpl newRequest(String authorization) {
        return new RequestImpl(null, new RequestStub(authorization), Collections.emptyList(), null);
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
        response = new ServletResponseSpy();
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
