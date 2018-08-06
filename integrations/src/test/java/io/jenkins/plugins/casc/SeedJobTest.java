package io.jenkins.plugins.casc;

import jenkins.model.Jenkins;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class SeedJobTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("SeedJobTest.yml")
    public void configure_seed_job() throws Exception {
        final Jenkins jenkins = Jenkins.getInstance();
        assertNotNull(jenkins.getItem("testJob1"));
        assertNotNull(jenkins.getItem("testJob2"));
    }
}
