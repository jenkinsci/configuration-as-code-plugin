package io.jenkins.plugins.casc;

import static jenkins.model.Jenkins.MANAGE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import hudson.model.User;
import hudson.security.ACL;
import hudson.security.ACLContext;
import java.util.List;
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
    }

    @Test
    public void testBuiltinActionsDiscovered() {
        List<String> urlNames = CasCManagementAction.all().stream()
                .map(CasCManagementAction::getUrlName)
                .toList();

        assertTrue("Built-in ReloadCasCAction must be discovered", urlNames.contains("reload"));
        assertTrue("Built-in ExportCasCAction must be discovered", urlNames.contains("viewExport"));
    }

    @Test
    public void testBuiltinReloadActionProperties() {
        CasCManagementAction reload = CasCManagementAction.all().stream()
                .filter(a -> "reload".equals(a.getUrlName()))
                .findFirst()
                .orElse(null);

        assertNotNull("ReloadCasCAction must be in the extension list", reload);
        assertTrue("Reload must require POST (CSRF protection)", reload.requiresPost());
        assertEquals("Reload must require MANAGE permission", Jenkins.MANAGE, reload.getRequiredPermission());
        assertNotNull("Reload should have an icon", reload.getIconFileName());
    }

    @Test
    public void testBuiltinExportActionProperties() {
        CasCManagementAction export = CasCManagementAction.all().stream()
                .filter(a -> "viewExport".equals(a.getUrlName()))
                .findFirst()
                .orElse(null);

        assertNotNull("ExportCasCAction must be in the extension list", export);
        assertTrue("Export must require POST (CSRF protection)", export.requiresPost());
        assertEquals("Export must require SYSTEM_READ permission", Jenkins.SYSTEM_READ, export.getRequiredPermission());
    }

    @Test
    public void testCustomStandaloneActionDiscovered() {
        List<String> urlNames = CasCManagementAction.all().stream()
                .map(CasCManagementAction::getUrlName)
                .toList();

        assertTrue("Custom StandaloneAction must be discovered", urlNames.contains("standalone-action"));
    }

    @Test
    public void testGetCustomActionsMatchesAll() {
        ConfigurationAsCode casc = ConfigurationAsCode.get();
        assertEquals(
                "getCustomActions() must return all registered CasCManagementAction extensions",
                CasCManagementAction.all().size(),
                casc.getCustomActions().size());
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
            assertNull(
                    "Stapler routing must return null if user lacks MANAGE permission",
                    casc.getDynamic("standalone-action"));
        }

        try (ACLContext ignored = ACL.as(User.getById("bob", true))) {
            Object routed = casc.getDynamic("standalone-action");
            assertNotNull("Stapler routing must return the action for privileged users", routed);
            assertTrue("Routed object should be the standalone action", routed instanceof StandaloneAction);

            assertNull("Stapler routing must return null for unregistered tokens", casc.getDynamic("does-not-exist"));
        }
    }

    @Test
    public void testGetDynamicReturnsBuiltinActionsWhenPermitted() {
        ConfigurationAsCode casc = ConfigurationAsCode.get();

        Object reload = casc.getDynamic("reload");
        assertNotNull("getDynamic(\"reload\") should return ReloadCasCAction when permitted", reload);
        assertEquals(
                "Returned action urlName must be 'reload'", "reload", ((CasCManagementAction) reload).getUrlName());

        Object export = casc.getDynamic("viewExport");
        assertNotNull("getDynamic(\"viewExport\") should return ExportCasCAction when permitted", export);
        assertEquals(
                "Returned action urlName must be 'viewExport'",
                "viewExport",
                ((CasCManagementAction) export).getUrlName());
    }

    @Test
    public void testReplaceDisabledDefaultFalse() {
        assertFalse(
                "replaceDisabled must be false when the system property is not set",
                ConfigurationAsCode.get().isReplaceDisabled());
    }

    @Test
    public void testReplaceDisabledTrueWhenPropertySet() {
        System.setProperty("casc.management.replace.disabled", "true");
        try {
            assertTrue(
                    "replaceDisabled must be true when casc.management.replace.disabled=true",
                    ConfigurationAsCode.get().isReplaceDisabled());
        } finally {
            System.clearProperty("casc.management.replace.disabled");
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
}
