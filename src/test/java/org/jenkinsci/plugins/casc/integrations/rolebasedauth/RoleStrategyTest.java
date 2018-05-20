package org.jenkinsci.plugins.casc.integrations.rolebasedauth;

import com.cloudbees.hudson.plugins.folder.Folder;
import com.michelin.cio.hudson.plugins.rolestrategy.Role;
import com.michelin.cio.hudson.plugins.rolestrategy.RoleBasedAuthorizationStrategy;
import hudson.model.Computer;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.User;
import hudson.security.AuthorizationStrategy;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.casc.Configurator;
import org.jenkinsci.plugins.casc.misc.ConfiguredWithCode;
import org.jenkinsci.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import static org.jenkinsci.plugins.casc.misc.PermissionAssert.*;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;

import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * @author Oleg Nenashev
 * @since TODO
 */
public class RoleStrategyTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    public void shouldReturnCustomConfigurator() {
        Configurator c = Configurator.lookup(RoleBasedAuthorizationStrategy.class);
        assertNotNull("Failed to find configurator for RoleBasedAuthorizationStrategy", c);
        assertEquals("Retrieved wrong configurator", RoleBasedAuthorizationStrategyConfigurator.class, c.getClass());
    }

    @Test
    @Issue("Issue #59")
    public void shouldReturnCustomConfiguratorForBaseType() {
        Configurator c = Configurator.lookupForBaseType(AuthorizationStrategy.class, "roleStrategy");
        assertNotNull("Failed to find configurator for RoleBasedAuthorizationStrategy", c);
        assertEquals("Retrieved wrong configurator", RoleBasedAuthorizationStrategyConfigurator.class, c.getClass());

        Configurator.lookup(RoleBasedAuthorizationStrategy.class);
    }

    @Test
    @Issue("Issue #48")
    @ConfiguredWithCode("RoleStrategy1.yml")
    public void shouldReadRolesCorrectly() throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        User admin = User.get("admin");
        User user1 = User.get("user1");
        User user2 = User.get("user2");
        Computer agent1 = j.jenkins.getComputer("agent1");
        Computer agent2 = j.jenkins.getComputer("agent2");
        Folder folderA = j.jenkins.createProject(Folder.class, "A");
        FreeStyleProject jobA1 = folderA.createProject(FreeStyleProject.class, "1");
        Folder folderB = j.jenkins.createProject(Folder.class, "B");
        FreeStyleProject jobB2 = folderB.createProject(FreeStyleProject.class, "2");

        AuthorizationStrategy s = j.jenkins.getAuthorizationStrategy();
        assertThat("Authorization Strategy has been read incorrectly",
                s, instanceOf(RoleBasedAuthorizationStrategy.class));
        RoleBasedAuthorizationStrategy rbas = (RoleBasedAuthorizationStrategy) s;

        Map<Role, Set<String>> globalRoles = rbas.getGrantedRoles(RoleBasedAuthorizationStrategy.GLOBAL);
        assertThat(globalRoles.size(), equalTo(2));

        // Admin has configuration access
        assertHasPermission(admin, j.jenkins, Jenkins.ADMINISTER, Jenkins.READ);
        assertHasPermission(user1, j.jenkins, Jenkins.READ);
        assertHasNoPermission(user1, j.jenkins, Jenkins.ADMINISTER, Jenkins.RUN_SCRIPTS);

        // Folder A is restricted to admin
        assertHasPermission(admin, folderA, Item.CONFIGURE);
        assertHasPermission(user1, folderA, Item.READ, Item.DISCOVER);
        assertHasNoPermission(user1, folderA, Item.CONFIGURE, Item.DELETE, Item.BUILD);

        // But they have access to jobs in Folder A
        assertHasPermission(admin, folderA, Item.CONFIGURE, Item.CANCEL);
        assertHasPermission(user1, jobA1, Item.READ, Item.DISCOVER, Item.CONFIGURE, Item.BUILD, Item.DELETE);
        assertHasPermission(user2, jobA1, Item.READ, Item.DISCOVER, Item.CONFIGURE, Item.BUILD, Item.DELETE);
        assertHasNoPermission(user1, folderA, Item.CANCEL);

        // FolderB is editable by user2, but he cannot delete it
        assertHasPermission(user2, folderB, Item.READ, Item.DISCOVER, Item.CONFIGURE, Item.BUILD);
        assertHasNoPermission(user2, folderB, Item.DELETE);
        assertHasNoPermission(user1, folderB, Item.CONFIGURE, Item.BUILD, Item.DELETE);

        // Only user1 can run on agent1, but he still cannot configure it
        assertHasPermission(admin, agent1, Computer.CONFIGURE, Computer.DELETE, Computer.BUILD);
        assertHasPermission(user1, agent1, Computer.BUILD);
        assertHasNoPermission(user1, agent1, Computer.CONFIGURE, Computer.DISCONNECT);

        // Same user still cannot build on agent2
        assertHasNoPermission(user1, agent2, Computer.BUILD);
    }

    @Test
    @Issue("Issue #214")
    @ConfiguredWithCode("RoleStrategy2.yml")
    public void shouldHandleNullItemsAndAgentsCorrectly() throws Exception {
        AuthorizationStrategy s = j.jenkins.getAuthorizationStrategy();
        assertThat("Authorization Strategy has been read incorrectly",
            s, instanceOf(RoleBasedAuthorizationStrategy.class));
        RoleBasedAuthorizationStrategy rbas = (RoleBasedAuthorizationStrategy) s;

        Map<Role, Set<String>> globalRoles = rbas.getGrantedRoles(RoleBasedAuthorizationStrategy.GLOBAL);
        assertThat(globalRoles.size(), equalTo(2));
    }
}
