package io.jenkins.plugins.casc;

import org.eclipse.jetty.server.Request;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LocalReloadCrumbExclusionTest {

    private Request newRequestWithPath(String hello) {
        return new Request(null, null) {
            @Override
            public String getPathInfo() {
                return hello;
            }
        };
    }

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();


    @Test
    public void crumbExclusionIsDisabledByDefault() throws Exception {
        environmentVariables.clear("CASC_ALLOW_LOCAL_RELOAD");

        LocalReloadCrumbExclusion crumbExclusion = new LocalReloadCrumbExclusion();

        assertFalse(crumbExclusion.process(newRequestWithPath("/reload-configuration-as-code/"), null, null));
    }

    @Test
    public void crumbExclusionChecksRequestPath() throws Exception {
        environmentVariables.set("CASC_ALLOW_LOCAL_RELOAD", "true");

        LocalReloadCrumbExclusion crumbExclusion = new LocalReloadCrumbExclusion();

        assertFalse(crumbExclusion.process(newRequestWithPath("/reload-configuration-as-code/2"), null, null));
    }

    @Test
    public void crumbExclustionAllowsReloadIfEnabledAndRequestPathMatch() throws Exception {
        environmentVariables.set("CASC_ALLOW_LOCAL_RELOAD", "true");

        LocalReloadCrumbExclusion crumbExclusion = new LocalReloadCrumbExclusion();

        AtomicBoolean callProcessed = new AtomicBoolean(false);
        assertTrue(crumbExclusion.process(newRequestWithPath("/reload-configuration-as-code/"), null, (request, response) -> callProcessed.set(true)));

        assertTrue(callProcessed.get());
    }
}