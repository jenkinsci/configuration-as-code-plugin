package io.jenkins.plugins.casc;

import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.ExtendedEmailPublisherDescriptor;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.yaml.YamlSource;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.tools.ant.filters.StringInputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.LoggerRule;

import static io.jenkins.plugins.casc.misc.Util.assertLogContains;
import static io.jenkins.plugins.casc.misc.Util.assertNotInLog;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
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

    @Test
    @Issue("SECURITY-1446")
    public void shouldProperlyRoundTripTokenMacro() throws Exception {
        final String defaultBody = "${PROJECT_NAME} - Build # ${BUILD_NUMBER} - ${BUILD_STATUS}:\n" +
                "Check console output at $BUILD_URL to view the results.";
        // This string contains extra escaping
        final String defaultSubject = "^^^${PROJECT_NAME} - Build # ^^${BUILD_NUMBER} - ^${BUILD_STATUS}!";

        ExtendedEmailPublisherDescriptor descriptor = ExtendedEmailPublisher.descriptor();
        descriptor.setDefaultBody(defaultBody);
        descriptor.setDefaultSubject(defaultSubject);

        // Verify that the variables get exported properly
        String exportedConfig = j.exportToString(false);
        assertThat("PROJECT_NAME should be escaped", exportedConfig, containsString("^${PROJECT_NAME}"));
        assertThat("BUILD_NUMBER should be escaped", exportedConfig, containsString("^${BUILD_NUMBER}"));
        assertThat("BUILD_STATUS should be escaped", exportedConfig, containsString("^${BUILD_STATUS}"));

        // Reimport the configuration
        ConfigurationAsCode.get().configureWith(YamlSource.of(new StringInputStream(exportedConfig)));
        assertLogContains(logging, "defaultBody =");
        assertLogContains(logging, "defaultSubject =");
        assertThat(ExtendedEmailPublisher.descriptor().getDefaultBody(), equalTo(defaultBody));
        assertThat(ExtendedEmailPublisher.descriptor().getDefaultSubject(), equalTo(defaultSubject));
    }
}
