package org.jenkinsci.plugins.casc;

import hudson.model.TopLevelItem;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.casc.misc.CodeConfiguratorRunner;
import org.jenkinsci.plugins.casc.misc.ConfiguredWithCode;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class SeedJobTest {

    public JenkinsRule j = new JenkinsRule();
    public CodeConfiguratorRunner config = new CodeConfiguratorRunner();

    @Rule
    public RuleChain chain = RuleChain.outerRule(j).around(config);

    @Test
    @ConfiguredWithCode("SeedJobTest.yml")
    public void configure_seed_job() throws Exception {
        final Jenkins jenkins = Jenkins.getInstance();
        final TopLevelItem test = jenkins.getItem("configuration-as-code");
        assertNotNull(test);
        assertTrue(test instanceof WorkflowMultiBranchProject);
    }
}
