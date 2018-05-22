package org.jenkinsci.plugins.casc;

import jenkins.model.Jenkins;
import org.jenkinsci.plugins.casc.misc.ConfiguredWithCode;
import org.jenkinsci.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EssentialsTest {
    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("EssentialsTest.yml")
    public void essentialsTest() throws Exception {
        final Jenkins jenkins = Jenkins.getInstance();
        assertEquals("Welcome to Jenkins Essentials!", jenkins.getSystemMessage());
    }
}
