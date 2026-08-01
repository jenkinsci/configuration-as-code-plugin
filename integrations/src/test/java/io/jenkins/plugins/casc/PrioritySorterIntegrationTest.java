package io.jenkins.plugins.casc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import hudson.ExtensionList;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import jenkins.advancedqueue.JobGroup;
import jenkins.advancedqueue.PriorityConfiguration;
import org.junit.Rule;
import org.junit.Test;

public class PrioritySorterIntegrationTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("priority-sorter.yml")
    public void testPrioritySorterConfigurationLoadsSuccessfully() {
        PriorityConfiguration config = ExtensionList.lookupSingleton(PriorityConfiguration.class);
        assertNotNull("PriorityConfiguration should be present", config);
        assertEquals("Should have exactly 1 job group", 1, config.getJobGroups().size());

        JobGroup group = config.getJobGroups().get(0);
        assertEquals("Important jobs", group.getDescription());
        assertEquals(1, group.getPriority());
        assertTrue("usePriorityStrategies should be true", group.isUsePriorityStrategies());
        assertEquals(
                "Should have successfully parsed 1 priority strategy",
                1,
                group.getPriorityStrategies().size());

        String strategyName = group.getPriorityStrategies()
                .get(0)
                .getPriorityStrategy()
                .getClass()
                .getSimpleName();
        assertEquals("JobPropertyStrategy", strategyName);
    }
}
