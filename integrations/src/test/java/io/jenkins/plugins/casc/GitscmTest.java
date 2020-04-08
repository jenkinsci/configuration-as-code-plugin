package io.jenkins.plugins.casc;

import hudson.ExtensionList;
import hudson.plugins.git.GitSCM;
import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author v1v (Victor Martinez)
 */
public class GitscmTest {

    @Rule
    public JenkinsConfiguredWithReadmeRule j = new JenkinsConfiguredWithReadmeRule();

    @Test
    @ConfiguredWithReadme("gitscm/README.md")
    public void configure_git() {
        final GitSCM.DescriptorImpl descriptor = ExtensionList.lookupSingleton(GitSCM.DescriptorImpl.class);
        assertNotNull(descriptor);
        assertEquals("jenkins", descriptor.getGlobalConfigName());
        assertEquals("jenkins@domain.local", descriptor.getGlobalConfigEmail());
        assertTrue(descriptor.isCreateAccountBasedOnEmail());
    }
}
