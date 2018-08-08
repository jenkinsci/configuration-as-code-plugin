package io.jenkins.plugins.casc;

import hudson.plugins.jira.JiraProjectProperty;
import hudson.plugins.jira.JiraProjectProperty.DescriptorImpl;
import hudson.plugins.jira.JiraSite;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.Assert;
import org.junit.Ignore;
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
    public void configure_jira_project_globalconfig() throws Exception {

        final DescriptorImpl descriptor = (DescriptorImpl) j.jenkins.getDescriptor(JiraProjectProperty.class);

        /** JENKINS-52906
         *  assertEquals(2, descriptor.getSites().length);
         *  assertEquals("http://jira.codehaus.org/", sites[0].getUrl().toString());
         *  assertEquals("http://issues.jenkins-ci.org/", sites[1].getUrl().toString());
         */

        assertEquals(1, descriptor.getSites().length);
        final JiraSite site = descriptor.getSites()[0];
        assertEquals("http://jira.codehaus.org/", site.getUrl().toString());
    }

}
