package org.jenkinsci.plugins.casc;

import hudson.model.ManagementLink;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.casc.misc.ConfiguredWithCode;
import org.jenkinsci.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

public class GetConfiguratorsTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode(value = "GetConfiguratorsTest.yml")
    public void shouldGetAllConfigurators() throws Exception {
        final Jenkins jenkins = Jenkins.getInstance();
        ConfigurationAsCode configurationAsCode = jenkins.getExtensionList(ManagementLink.class)
                .get(ConfigurationAsCode.class);
        assertThat(configurationAsCode.getConfigurators(), hasSize(greaterThan(0)));
    }
}
