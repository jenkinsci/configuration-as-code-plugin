package io.jenkins.plugins.casc;

import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import java.io.StringWriter;
import org.apache.commons.io.output.WriterOutputStream;
import org.jenkinsci.plugins.sge.BatchCloud;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class SGECloudTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @Issue("SECURITY-1458")
    public void shouldNotExportPassword() throws Exception {
        ConfigurationAsCode casc = ConfigurationAsCode.get();

        final String passwordText = "Hello, world!";
        BatchCloud cloud = new BatchCloud("testBatchCloud", "whatever",
                "sge", 5, "sge.acmecorp.com", 8080,
                "username", passwordText);
        j.jenkins.clouds.add(cloud);

        StringWriter writer = new StringWriter();
        casc.export(new WriterOutputStream(writer));
        String exported = writer.toString();
        assertThat("Password should not have been exported",
                exported, not(containsString(passwordText)));
    }
}
