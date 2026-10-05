package io.jenkins.plugins.casc;

import static jenkins.model.Jenkins.MANAGE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import hudson.model.User;
import hudson.security.ACL;
import hudson.security.ACLContext;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.TestExtension;

public class CasCManagementActionTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testInterfaceDefaults() {
        CasCManagementAction defaultAction = new CasCManagementAction() {
            @Override
            public String getIconFileName() {
                return null;
            }

            @Override
            public String getDisplayName() {
                return "Default Test";
            }

            @Override
            public String getUrlName() {
                return "default-test";
            }
        };

        assertTrue("Actions should require POST by default for CSRF protection", defaultAction.requiresPost());
        assertEquals(
                "Actions should require MANAGE permission by default", MANAGE, defaultAction.getRequiredPermission());
        assertNull("Actions should not override anything by default", defaultAction.getOverridesAction());
    }

    @Test
    public void testExtensionDiscovery() {
        assertEquals(
                "Should discover both TestExtension actions",
                2,
                CasCManagementAction.all().size());
    }

    @Test
    public void testGetOverrideFor() {
        ConfigurationAsCode casc = ConfigurationAsCode.get();

        CasCManagementAction reloadOverride = casc.getOverrideFor("reload");
        assertNotNull("Should find the override for 'reload'", reloadOverride);
        assertEquals("custom-reload", reloadOverride.getUrlName());
        assertTrue("Should be an instance of OverrideReloadAction", reloadOverride instanceof OverrideReloadAction);

        CasCManagementAction exportOverride = casc.getOverrideFor("viewExport");
        assertNull("Should return null when no extension overrides 'viewExport'", exportOverride);
    }

    @Test
    public void testGetDynamicRoutingAndSecurity() {
        ConfigurationAsCode casc = ConfigurationAsCode.get();

        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        MockAuthorizationStrategy auth = new MockAuthorizationStrategy()
                .grant(Jenkins.READ)
                .everywhere()
                .to("alice")
                .grant(Jenkins.READ, MANAGE)
                .everywhere()
                .to("bob");
        j.jenkins.setAuthorizationStrategy(auth);

        try (ACLContext ignored = ACL.as(User.getById("alice", true))) {
            Object routed = casc.getDynamic("standalone-action");
            assertNull("Stapler routing must return null if user lacks the required permission", routed);
        }

        try (ACLContext ignored = ACL.as(User.getById("bob", true))) {
            Object routed = casc.getDynamic("standalone-action");
            assertNotNull("Stapler routing must return the action for privileged users", routed);
            assertTrue("Routed object should be the standalone action", routed instanceof StandaloneAction);

            Object invalidRoute = casc.getDynamic("does-not-exist");
            assertNull("Stapler routing must return null for unregistered tokens", invalidRoute);
        }
    }

    @TestExtension
    public static class StandaloneAction implements CasCManagementAction {
        @Override
        public String getIconFileName() {
            return "symbol-plug";
        }

        @Override
        public String getDisplayName() {
            return "Standalone Tool";
        }

        @Override
        public String getUrlName() {
            return "standalone-action";
        }
    }

    @TestExtension
    public static class OverrideReloadAction implements CasCManagementAction {
        @Override
        public String getIconFileName() {
            return null;
        }

        @Override
        public String getDisplayName() {
            return "Custom Reload";
        }

        @Override
        public String getUrlName() {
            return "custom-reload";
        }

        @Override
        public String getOverridesAction() {
            return "reload";
        }

        @Override
        public boolean requiresPost() {
            return false;
        }
    }
}
