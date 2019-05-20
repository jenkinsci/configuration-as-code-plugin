package io.jenkins.plugins.casc;

import hudson.model.ListView;
import hudson.views.WeatherColumn;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import io.jenkins.plugins.casc.model.Sequence;
import org.jenkinsci.plugins.github_branch_source.GitHubBranchFilter;
import org.jenkinsci.plugins.github_branch_source.GitHubPullRequestFilter;
import org.junit.ClassRule;
import org.junit.Test;

import static io.jenkins.plugins.casc.misc.Util.getJenkinsRoot;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ListViewTest {

    @ClassRule
    @ConfiguredWithCode("ListViewTest.yml")
    public static JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    public void should_import() {
        ListView view = (ListView) j.jenkins.getView("test-list-view");
        assertNotNull(view);
        assertThat(view.getDescription(), is("so descriptive!"));
        assertThat(view.getIncludeRegex(), is("a.+"));
        assertThat(view.getJobNames(), hasItems("otherTest", "test"));
        GitHubBranchFilter gitHubBranchFilter = view.getJobFilters().get(GitHubBranchFilter.class);
        GitHubPullRequestFilter gitHubPullRequestFilter = view.getJobFilters().get(GitHubPullRequestFilter.class);
        assertNotNull(gitHubBranchFilter);
        assertNotNull(gitHubPullRequestFilter);
        assertThat(view.getColumns().size(), is(6));
        WeatherColumn weatherColumn = view.getColumns().get(WeatherColumn.class);
        assertNull(weatherColumn);
        assertThat(view.getStatusFilter(), is(true));
        assertThat(view.isRecurse(), is(true));
    }

    @Test
    public void should_export() throws Exception {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        final Mapping configNode = getJenkinsRoot(context);
        final CNode viewsNode = configNode.get("views");
        Mapping listView = viewsNode.asSequence().get(1).asMapping().get("list").asMapping();
        assertThat(listView.getScalarValue("name"), is("test-list-view"));
        assertThat(listView.getScalarValue("description"), is("so descriptive!"));
        assertThat(listView.getScalarValue("includeRegex"), is("a.+"));
        assertThat(listView.getScalarValue("statusFilter"), is("true"));
        assertThat(listView.getScalarValue("recurse"), is("true"));
        Sequence listViewColumns = listView.get("columns").asSequence();
        assertNotNull(listViewColumns);
        assertEquals(6, listViewColumns.size());
        assertThat(listViewColumns.get(0).asScalar().getValue(), is("status"));
        assertThat(listViewColumns.get(1).asScalar().getValue(), is("jobName"));
        assertThat(listViewColumns.get(2).asScalar().getValue(), is("lastSuccess"));
        assertThat(listViewColumns.get(3).asScalar().getValue(), is("lastFailure"));
        assertThat(listViewColumns.get(4).asScalar().getValue(), is("lastDuration"));
        assertThat(listViewColumns.get(5).asScalar().getValue(), is("buildButton"));
        Sequence jobFilters = listView.get("jobFilters").asSequence();
        assertThat(jobFilters.get(0).asScalar().getValue(), is("gitHubBranchFilter"));
        assertThat(jobFilters.get(1).asScalar().getValue(), is("gitHubPullRequestFilter"));
        Sequence jobNames = listView.get("jobNames").asSequence();
        assertThat(jobNames.get(0).asScalar().getValue(), is("otherTest"));
        assertThat(jobNames.get(1).asScalar().getValue(), is("test"));
    }
}
