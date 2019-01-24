package io.jenkins.plugins.casc;

import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.apache.commons.httpclient.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.jvnet.hudson.test.LoggerRule;
import org.kohsuke.stapler.RequestImpl;
import org.kohsuke.stapler.ResponseImpl;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.junit.Assert.*;

public class LocalReloadActionTest {

    private Date lastTimeLoaded;

    private class ServletResponseSpy extends Response {

        private int error = HttpStatus.SC_OK;

        public ServletResponseSpy() {
            super(null, null);
        }

        @Override
        public void sendError(int sc) throws IOException {
            error = sc;
        }


        @Override
        public int getStatus() {
            return error;
        }
    }

    private class RequestStub extends Request {
        private final String localAddr;
        private final String remoteAddr;

        public RequestStub(String localAddr, String remoteAddr) {
            super(null, null);
            this.localAddr = localAddr;
            this.remoteAddr = remoteAddr;
        }

        @Override
        public String getLocalAddr() {
            return localAddr;
        }

        @Override
        public String getRemoteAddr() {
            return remoteAddr;
        }
    }

    private LocalReloadAction localReloadAction;

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Rule
    public final LoggerRule loggerRule = new LoggerRule();

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    private ServletResponseSpy response;


    private ResponseImpl newResponse() {
        return new ResponseImpl(null, response);
    }

    private RequestImpl newRequest(String localAddr, String remoteAddr) {
        return new RequestImpl(null, new RequestStub(localAddr, remoteAddr), Collections.emptyList(), null);
    }

    private boolean configWasReloaded() {
        return !lastTimeLoaded.equals(ConfigurationAsCode.get().getLastTimeLoaded());
    }

    @Before
    public void setUp() throws Exception {
        localReloadAction = new LocalReloadAction();
        response = new ServletResponseSpy();
        loggerRule.record(LocalReloadAction.class, Level.ALL);
        loggerRule.capture(3);
        lastTimeLoaded = ConfigurationAsCode.get().getLastTimeLoaded();
    }

    @Test
    public void getUrlName() {
        localReloadAction.getUrlName();
        assertEquals("/reload-configuration-as-code", localReloadAction.getUrlName());
    }

    @Test
    public void reloadIsDisabledByDefault() throws IOException {
        environmentVariables.clear("CASC_ALLOW_LOCAL_RELOAD");

        localReloadAction.doIndex(null, null);

        List<LogRecord> messages = loggerRule.getRecords();
        assertEquals(1, messages.size());
        assertEquals("local reload is not enabled", messages.get(0).getMessage());
        assertEquals(Level.FINE, messages.get(0).getLevel());
        assertFalse(configWasReloaded());
    }


    @Test
    public void reloadReturnsUnauthorizedIfLocalAndRemoteAddressDoNotMatch() throws IOException {
        environmentVariables.set("CASC_ALLOW_LOCAL_RELOAD", "true");

        localReloadAction.doIndex(newRequest("local", "remote"), new ResponseImpl(null, response));

        assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatus());

        List<LogRecord> messages = loggerRule.getRecords();
        assertEquals(1, messages.size());
        assertEquals("unauthorized access from 'remote'", messages.get(0).getMessage());
        assertEquals(Level.WARNING, messages.get(0).getLevel());
        assertFalse(configWasReloaded());
    }

    @Test
    public void reloadReturnsOkIfLocalAndRemoteAddressAreIdentical() throws IOException {
        environmentVariables.set("CASC_ALLOW_LOCAL_RELOAD", "true");
        assertFalse(configWasReloaded());

        localReloadAction.doIndex(newRequest("remote", "remote"), newResponse());

        assertEquals(HttpStatus.SC_OK, response.getStatus());

        List<LogRecord> messages = loggerRule.getRecords();
        assertEquals(1, messages.size());
        assertEquals("local reload triggered from 'remote'", messages.get(0).getMessage());
        assertEquals(Level.INFO, messages.get(0).getLevel());

        assertTrue(configWasReloaded());
    }

}