package io.jenkins.plugins.casc;

import hudson.init.InitMilestone;
import hudson.init.Initializer;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import java.io.File;
import java.io.IOException;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.TestExtension;

import static org.junit.Assert.assertFalse;

public class InitTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @Issue("Issue #280 - https://github.com/jenkinsci/configuration-as-code-plugin/issues/280")
    @ConfiguredWithCode("init.yml")
    public void testInitializer() {
        File flag = new File(j.jenkins.getRootDir(), "gitTool-flag.txt");
        assertFalse("Possible collision between CasC initialization and Jobs load", flag.exists());

    }

    @TestExtension("testInitializer")
    public static class JobsDependantOnConfig {

        @Initializer(after = InitMilestone.SYSTEM_CONFIG_ADAPTED, before = InitMilestone.JOB_LOADED)
        public void happensWhenJobLoading() throws IOException, InterruptedException {
            Jenkins jenkins = Jenkins.get();
            File gitTools = new File(jenkins.getRootDir(), "hudson.plugins.git.GitTool.xml");
            int tries = 0;
            while (!gitTools.exists() && tries++ < 10) {
                File flag = new File(jenkins.getRootDir(), "gitTool-flag.txt");
                if (!flag.exists()) {
                    flag.createNewFile();
                    Thread.sleep(5_000L);
                }
            }
        }

    }
}
