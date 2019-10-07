package io.jenkins.plugins.casc;

import hudson.ExtensionList;
import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import org.jenkinsci.plugins.ewm.steps.ExwsAllocateStep;
import org.jenkinsci.plugins.ewm.steps.ExwsStep;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

/**
 * @author <a href="mailto:VictorMartinezRubio@gmail.com">Victor Martinez</a>
 */
public class ExternalWorkspaceManagerTest {

    @Rule
    public JenkinsConfiguredWithReadmeRule j = new JenkinsConfiguredWithReadmeRule();

    @Test
    @ConfiguredWithReadme("external-workspace-manager/README.md")
    public void configure_external_workspace_manager() throws Exception {
        // Already validated in the plugin itself:
        // https://github.com/jenkinsci/external-workspace-manager-plugin/pull/68

        // Let's run some basic validations
        ExwsAllocateStep.DescriptorImpl descriptor =  ExtensionList.lookupSingleton(ExwsAllocateStep.DescriptorImpl.class);
        assertThat(descriptor.getDiskPools(), hasSize(1));

        ExwsStep.DescriptorImpl globalTemplateDescriptor = ExtensionList.lookupSingleton(ExwsStep.DescriptorImpl.class);
        assertThat(globalTemplateDescriptor.getTemplates(), hasSize(5));
    }
}
