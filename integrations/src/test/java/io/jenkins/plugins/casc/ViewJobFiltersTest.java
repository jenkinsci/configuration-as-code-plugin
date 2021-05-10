package io.jenkins.plugins.casc;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.util.List;

import hudson.views.BuildDurationFilter;
import hudson.views.BuildStatusFilter;
import hudson.views.JobTypeFilter;
import hudson.views.SecurityFilter;

import org.jfrog.hudson.ArtifactoryBuilder;
import org.jfrog.hudson.ArtifactoryServer;
import org.junit.Rule;
import org.junit.Test;

import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import jenkins.model.Jenkins;

public class ViewJobFiltersTest {
	
	@Rule
    public JenkinsConfiguredWithReadmeRule j = new JenkinsConfiguredWithReadmeRule();
	
	@Test
    @ConfiguredWithReadme(value = "view-job-filters/README.md")
    public void configure_view_job_filters() throws Exception {
		
		final BuildDurationFilter buildDurationFilter = new BuilDurationFilter();
		assertTrue(buildDurationFilter.isLessThan());
		assertThat(buildDurationFilter.getBuildDurationMinutes, is(5));
		assertThat(buildDurationFilter.getAmount(), is(60.0));
		assertThat(buildDurationFilter.getAmountTypeString(), is("Days"));
		assertThat(buildDurationFilter.getBuildCountTypeString(), is("Latest"));
		assertTrue(buildDurationFilter.isIncludeMatched());	
		
		final BuildStatusFilter buildStatusFilter = new BuildStatusFilter();
		assertTrue(buildStatusFilter.isNeverBuilt());
		assertFalse(buildStatusFilter.isBuilding());
		assertTrue(buildStatusFilter.isInBuildQueue());
		assertTrue(buildStatusFilter.isIncludeMatched());
		
		final JobTypeFilter jobTypeFilter = new JobTypeFilter();
		assertTrue(buildStatusFilter.isIncludeMatched());
		
		final SecurityFilter securityFilter = new SecurityFilter();
		assertThat(securityFilter.getPermissionCheckType(), is("MustMatchAll"));
		assertTrue(securityFilter.isConfigure());
		assertFalse(securityFilter.isBuild());
		assertFalse(securityFilter.isWorkspace());
		assertTrue(buildStatusFilter.isIncludeMatched());
    }
}
