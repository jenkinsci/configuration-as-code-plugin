package io.jenkins.plugins.casc;

import hudson.plugins.emailext.ExtendedEmailPublisher;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.LoggerRule;

import static io.jenkins.plugins.casc.misc.Util.assertLogContains;
import static io.jenkins.plugins.casc.misc.Util.assertNotInLog;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class MailExtTest {

    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();
    public LoggerRule logging = new LoggerRule();

    @Rule
    public RuleChain chain= RuleChain
            .outerRule(logging.record(Logger.getLogger(Attribute.class.getName()), Level.INFO).capture(2048))
            .around(j);

    private static final String SMTP_PASSWORD = "myPassword";

    @Test
    @ConfiguredWithCode("MailExtTest.yml")
    @Issue("SECURITY-1404")
    public void shouldNotExportOrLogCredentials() throws Exception {
        assertEquals(SMTP_PASSWORD, ExtendedEmailPublisher.descriptor().getSmtpPassword().getPlainText());
        assertLogContains(logging, "smtpPassword =");
        assertNotInLog(logging, SMTP_PASSWORD);

        // Verify that the password does not get exported
        String exportedConfig = j.exportToString(false);
        assertThat("No entry was exported for SMTP credentials", exportedConfig, containsString("smtpPassword"));
        assertThat("There should be no SMTP password in the exported YAML", exportedConfig, not(containsString(SMTP_PASSWORD)));
    }
}
