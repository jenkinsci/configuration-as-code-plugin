package io.jenkins.plugins.casc.yaml;

import hudson.ExtensionList;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MergeStrategyTest {

    @Rule
    public RestoreSystemProperties properties = new RestoreSystemProperties();
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void haveTheDefaultStrategy() {
        ExtensionList<MergeStrategy> strategyExtensionList = Jenkins.getInstance()
            .getExtensionList(MergeStrategy.class);

        assertTrue("should have at least one strategy",
            strategyExtensionList.size() > 0);
        assertEquals("default merge strategy's name should be default",
            "default", MergeStrategyFactory.getMergeStrategy().getName());
    }
}
