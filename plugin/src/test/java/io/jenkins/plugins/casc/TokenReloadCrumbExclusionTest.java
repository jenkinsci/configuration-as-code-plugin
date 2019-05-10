package io.jenkins.plugins.casc;

import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.jetty.server.Request;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TokenReloadCrumbExclusionTest {

    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    private Request newRequestWithPath(String hello) {
        return new Request(null, null) {
            @Override
            public String getPathInfo() {
                return hello;
            }
        };
    }

    @Test
    public void crumbExclusionIsDisabledByDefault() throws Exception {
        System.clearProperty("casc.reload.token");

        TokenReloadCrumbExclusion crumbExclusion = new TokenReloadCrumbExclusion();

        assertFalse(crumbExclusion.process(newRequestWithPath("/reload-configuration-as-code/"), null, null));
    }

    @Test
    public void crumbExclusionChecksRequestPath() throws Exception {
        System.setProperty("casc.reload.token", "someSecretValue");

        TokenReloadCrumbExclusion crumbExclusion = new TokenReloadCrumbExclusion();

        assertFalse(crumbExclusion.process(newRequestWithPath("/reload-configuration-as-code/2"), null, null));
    }

    @Test
    public void crumbExclustionAllowsReloadIfEnabledAndRequestPathMatch() throws Exception {
        System.setProperty("casc.reload.token", "someSecretValue");

        TokenReloadCrumbExclusion crumbExclusion = new TokenReloadCrumbExclusion();

        AtomicBoolean callProcessed = new AtomicBoolean(false);
        assertTrue(crumbExclusion.process(newRequestWithPath("/reload-configuration-as-code/"), null, (request, response) -> callProcessed.set(true)));

        assertTrue(callProcessed.get());
    }
}
