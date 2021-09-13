package io.jenkins.plugins.casc;

import hudson.ExtensionList;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import java.util.List;
import jenkins.metrics.api.MetricsAccessKey;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EssentialsTest {
    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("EssentialsTest.yml")
    public void essentialsTest() {
        final Jenkins jenkins = Jenkins.get();
        assertEquals("Welcome to Jenkins Essentials!", jenkins.getSystemMessage());

        final ExtensionList<MetricsAccessKey.DescriptorImpl> metricsDescriptors = ExtensionList.lookup(MetricsAccessKey.DescriptorImpl.class);
        assertNotNull(metricsDescriptors);
        assertThat(metricsDescriptors, hasSize(1));

        MetricsAccessKey.DescriptorImpl metricsDescriptor = metricsDescriptors.get(0);

        final List<MetricsAccessKey> accessKeys = metricsDescriptor.getAccessKeys();
        assertThat(accessKeys, hasSize(1));

        MetricsAccessKey accessKey = accessKeys.get(0);
        assertThat(accessKey.getKey(), is("evergreen"));
        assertThat(accessKey.getDescription(), is("Key for evergreen health-check"));
        assertThat(accessKey.isCanHealthCheck(), is(true));
        assertThat(accessKey.isCanPing(), is(false));
        assertThat(accessKey.isCanPing(), is(false));
        assertThat(accessKey.isCanThreadDump(), is(false));
        assertThat(accessKey.isCanMetrics(), is(false));
        assertThat(accessKey.getOrigins(), is("*"));

        //metricsDescriptor.g
    }
}
