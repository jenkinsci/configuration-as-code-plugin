package io.jenkins.plugins.casc;

import hudson.ExtensionList;
import hudson.tasks.Maven;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import jenkins.model.Jenkins;
import jenkins.mvn.GlobalMavenConfig;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class GlobalToolGlobalMavenTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Issue("JENKINS-62446")
    @Test
    @ConfiguredWithCode("GlobalToolGlobalMavenTest.yaml")
    public void mavenGlobalConfigurationShouldBeConfigurable() {
        final Jenkins jenkins = Jenkins.get();
        //The one with @Symbol("maven")
        ExtensionList<Maven.DescriptorImpl> mavenTool = jenkins
            .getExtensionList(Maven.DescriptorImpl.class);

        assertThat("An installation with 'custom' name should have been configured.",
            mavenTool.get(0).getInstallations()[0].getName(), is("custom"));

        //The one with @Symbol("mavenGlobalConfig") since TODO
        ExtensionList<GlobalMavenConfig> globalMavenConfig = jenkins
            .getExtensionList(GlobalMavenConfig.class);

        //TODO find how to test the configuration.

    }

}
