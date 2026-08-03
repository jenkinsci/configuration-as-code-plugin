package io.jenkins.plugins.casc.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import hudson.model.Action;
import hudson.model.FreeStyleProject;
import java.util.Collection;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class ExportItemActionFactoryTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testCreateForSupportedItem() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("supported-job");

        ExportItemActionFactory factory = new ExportItemActionFactory();
        Collection<? extends Action> actions = factory.createFor(project);

        assertEquals(1, actions.size());
        assertTrue("Should return an ExportItemAction", actions.iterator().next() instanceof ExportItemAction);
    }
}
