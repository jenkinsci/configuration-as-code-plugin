package io.jenkins.plugins.casc.yaml;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import hudson.ExtensionList;
import jenkins.model.Jenkins;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class MergeStrategyTest {

    @Test
    void haveTheDefaultStrategy(JenkinsRule j) {
        ExtensionList<MergeStrategy> strategyExtensionList = Jenkins.get().getExtensionList(MergeStrategy.class);

        assertFalse(strategyExtensionList.isEmpty(), "should have at least one strategy");
        assertNotNull(
                MergeStrategyFactory.getMergeStrategyOrDefault(null), "default merge strategy should not be null");
    }
}
