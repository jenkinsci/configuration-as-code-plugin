package io.jenkins.plugins.casc;

import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import jenkins.model.Jenkins;

import com.checkmarx.jenkins.CxScanBuilder;

import org.junit.Test;

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
