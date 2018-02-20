package org.jenkinsci.plugins.casc.configurationsource;

import hudson.model.ManagementLink;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.casc.ConfigurationAsCode;
import org.jenkinsci.plugins.casc.misc.EnvVarsRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.jvnet.hudson.test.JenkinsRule;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by mads on 2/20/18.
 */
public class ConfigurationSourceTest {

    @Rule
    public RuleChain withEnv = RuleChain.outerRule(
            new EnvVarsRule()
            .env("CASC_JENKINS_CONFIG", ConfigurationSourceTest.class.getResource("FileConfigurationSource.yml").getPath()))
            .around(new JenkinsRule());

    @Test
    public void testThatConfigurationSourceAreAvailable() {
        assertThat(ConfigurationSource.all().isEmpty(), is(false));
    }

    @Test
    public void testThatSourcesHasBeenSet() {
        assertThat(ConfigurationAsCode.get().getSources().size(), is(1));
        assertThat(ConfigurationAsCode.get().getSources(), hasItem("FileConfigurationSource.yml"));
    }
}
