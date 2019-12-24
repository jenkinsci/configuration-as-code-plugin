package io.jenkins.plugins.casc;

import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.RuleChain;

import static org.junit.Assert.assertEquals;

public class OrderedMergeStrategyTest {

    @Rule
    public RuleChain chain = RuleChain.outerRule(new EnvironmentVariables()
        .set("CASC_MERGE_STRATEGY", "order"))
        .around(new JenkinsConfiguredWithCodeRule());

    @Test
    @ConfiguredWithCode(value = {"merge1.yml", "merge2.yml"})
    public void orderMergeStrategy() {
        int executorNum = Jenkins.getInstance().getNumExecutors();
        assertEquals("only use the last config file", 20, executorNum);
    }
}
