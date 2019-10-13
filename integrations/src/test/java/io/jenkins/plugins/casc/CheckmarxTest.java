package io.jenkins.plugins.casc;

import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import java.util.List;
import jenkins.model.Jenkins;

import com.checkmarx.jenkins.CxScanBuilder;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.RuleChain;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

public class CheckmarxTest {

    @Test
    @ConfiguredWithReadme(value = "checkmarx/README.md")
    public void configure_artifactory() throws Exception {
        final Jenkins jenkins = Jenkins.get();
        final CxScanBuilder.DescriptorImpl cxScanConfig = (CxScanBuilder.DescriptorImpl) jenkins.getDescriptor(CxScanBuilder.class);
        assertTrue(descriptor.getUseCredentialsPlugin());

        assertThat(cxScanConfig.getServerUrl(), is(equalTo("https://checkmarx.url.tld")));
    }
}
