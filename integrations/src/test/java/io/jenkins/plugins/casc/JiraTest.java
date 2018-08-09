package io.jenkins.plugins.casc;

import hudson.plugins.jira.JiraProjectProperty;
import hudson.plugins.jira.JiraProjectProperty.DescriptorImpl;
import hudson.plugins.jira.JiraSite;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class JiraTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("JiraTest.yml")
    public void configure_jira_project_globalconfig() throws Exception {

        final DescriptorImpl descriptor = (DescriptorImpl) j.jenkins.getDescriptor(JiraProjectProperty.class);

        final JiraSite[] sites = descriptor.getSites();
        assertEquals(2, sites.length);
        assertEquals("http://jira.codehaus.org/", sites[0].getUrl().toString());
        assertEquals("http://issues.jenkins-ci.org/", sites[1].getUrl().toString());
    }

}
