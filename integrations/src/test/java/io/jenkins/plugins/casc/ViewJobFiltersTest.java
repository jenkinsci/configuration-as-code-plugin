package io.jenkins.plugins.casc;

import hudson.model.Descriptor;
import hudson.model.ListView;
import hudson.util.DescribableList;
import hudson.views.BuildDurationFilter;
import hudson.views.BuildStatusFilter;
import hudson.views.SecurityFilter;
import hudson.views.ViewJobFilter;
import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ViewJobFiltersTest {

    @Rule
    public JenkinsConfiguredWithReadmeRule j = new JenkinsConfiguredWithReadmeRule();

    @Test
    @ConfiguredWithReadme(value = "view-job-filters/README.md")
    public void configure_view_job_filters() throws Exception {
        BuildDurationFilter duration = null;
        BuildStatusFilter status = null;
        SecurityFilter security = null;

        Jenkins jenkins = j.jenkins;
        ListView listView = (ListView) jenkins.getView("MyFirstView");
        DescribableList<ViewJobFilter, Descriptor<ViewJobFilter>> a = listView.getJobFilters();
        duration = a.get(BuildDurationFilter.class);
        status = a.get(BuildStatusFilter.class);
        security = a.get(SecurityFilter.class);

        final BuildDurationFilter buildDurationFilter = duration;
        assertTrue(buildDurationFilter.isLessThan());
        assertThat(buildDurationFilter.getBuildDurationMinutes(), is("5"));
        assertThat(buildDurationFilter.getAmount(), is(60.0f));
        assertThat(buildDurationFilter.getAmountTypeString(), is("Days"));
        assertThat(buildDurationFilter.getBuildCountTypeString(), is("Latest"));
        assertTrue(buildDurationFilter.isIncludeMatched());

        final BuildStatusFilter buildStatusFilter = status;
        assertTrue(buildStatusFilter.isNeverBuilt());
        assertFalse(buildStatusFilter.isBuilding());
        assertTrue(buildStatusFilter.isInBuildQueue());
        assertTrue(buildStatusFilter.isIncludeMatched());

        final SecurityFilter securityFilter = security;
        assertThat(securityFilter.getPermissionCheckType(), is("MustMatchAll"));
        assertTrue(securityFilter.isConfigure());
        assertFalse(securityFilter.isBuild());
        assertFalse(securityFilter.isWorkspace());
        assertTrue(buildStatusFilter.isIncludeMatched());
    }
}
