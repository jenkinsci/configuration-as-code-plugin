package io.jenkins.plugins.casc;

import hudson.plugins.jira.JiraGlobalConfiguration;
import hudson.plugins.jira.JiraSite;
import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class JiraTest {

    @Rule
    public JenkinsConfiguredWithReadmeRule j = new JenkinsConfiguredWithReadmeRule();

    @Test
    @ConfiguredWithReadme("jira/README.md")
    @Issue("JENKINS-52906")
    public void configure_jira_project_globalconfig() throws Exception {

        List<JiraSite> sites = JiraGlobalConfiguration.get().getSites();
        // Was failing due to JENKINS-52906
        assertThat(sites, hasSize(2));
        assertEquals("http://jira.codehaus.org/", sites.get(0).getUrl().toString());
        assertEquals("http://issues.jenkins.io/", sites.get(1).getUrl().toString());
    }

}
