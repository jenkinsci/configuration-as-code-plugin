package io.jenkins.plugins.casc;

import hudson.tasks.Mailer;
import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class MailerTest {

    @Rule
    public JenkinsConfiguredWithReadmeRule j = new JenkinsConfiguredWithReadmeRule();

    @Test
    @ConfiguredWithReadme("mailer/README.md")
    public void configure_mailer() throws Exception {
        final Jenkins jenkins = Jenkins.get();
        final Mailer.DescriptorImpl descriptor = (Mailer.DescriptorImpl) jenkins.getDescriptor(Mailer.class);
        assertEquals("4441", descriptor.getSmtpPort());
        assertEquals("do-not-reply@acme.org", descriptor.getReplyToAddress());
        assertEquals("smtp.acme.org", descriptor.getSmtpHost() );

        // FIXME setAdminAddress is deprecated and should NOT be set this way
        // see https://github.com/jenkinsci/mailer-plugin/pull/39
        assertEquals("admin@acme.org", descriptor.getAdminAddress());
    }
}
