package org.jenkinsci.plugins.casc.integrations.jobdsl;

import hudson.model.TopLevelItem;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.casc.misc.ConfiguredWithCode;
import org.jenkinsci.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class SeedJobTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("SeedJobTest.yml")
    public void configure_seed_job() throws Exception {
        final Jenkins jenkins = Jenkins.getInstance();
        final TopLevelItem test = jenkins.getItem("configuration-as-code");
        assertNotNull(test);
        assertTrue(test instanceof WorkflowMultiBranchProject);
    }
}
