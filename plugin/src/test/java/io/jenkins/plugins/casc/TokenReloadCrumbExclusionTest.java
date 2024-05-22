package io.jenkins.plugins.casc;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;

public class TokenReloadCrumbExclusionTest {

    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    @Test
    public void crumbExclusionIsDisabledByDefault() throws Exception {
        System.clearProperty("casc.reload.token");

        TokenReloadCrumbExclusion crumbExclusion = new TokenReloadCrumbExclusion();

        assertFalse(crumbExclusion.process(new MockHttpServletRequest("/reload-configuration-as-code/"), null, null));
    }

    @Test
    public void crumbExclusionChecksRequestPath() throws Exception {
        System.setProperty("casc.reload.token", "someSecretValue");

        TokenReloadCrumbExclusion crumbExclusion = new TokenReloadCrumbExclusion();

        assertFalse(crumbExclusion.process(new MockHttpServletRequest("/reload-configuration-as-code/2"), null, null));
    }

    @Test
    public void crumbExclustionAllowsReloadIfEnabledAndRequestPathMatch() throws Exception {
        System.setProperty("casc.reload.token", "someSecretValue");

        TokenReloadCrumbExclusion crumbExclusion = new TokenReloadCrumbExclusion();

        AtomicBoolean callProcessed = new AtomicBoolean(false);
        assertTrue(crumbExclusion.process(
                new MockHttpServletRequest("/reload-configuration-as-code/"),
                null,
                (request, response) -> callProcessed.set(true)));

        assertTrue(callProcessed.get());
    }
}
