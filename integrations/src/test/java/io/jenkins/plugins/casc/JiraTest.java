package io.jenkins.plugins.casc;

import hudson.plugins.jira.JiraProjectProperty;
import hudson.plugins.jira.JiraProjectProperty.DescriptorImpl;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class JiraTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("JiraTest.yml")
    @Issue("JENKINS-52906")
    public void configure_jira_project_globalconfig() throws Exception {

        final DescriptorImpl descriptor = (DescriptorImpl) j.jenkins.getDescriptor(JiraProjectProperty.class);

        // Was failing due to JENKINS-52906
        assertEquals(2, descriptor.getSites().length);
        assertEquals("http://jira.codehaus.org/", descriptor.getSites()[0].getUrl().toString());
        assertEquals("http://issues.jenkins.io/", descriptor.getSites()[1].getUrl().toString());
    }

}
