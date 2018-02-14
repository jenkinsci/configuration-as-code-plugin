package org.jenkinsci.plugins.casc;

import hudson.tasks.Mailer;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.casc.misc.ConfiguredWithCode;
import org.jenkinsci.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class MailerTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("MailerTest.yml")
    public void configure_mailer() throws Exception {
        final Jenkins jenkins = Jenkins.getInstance();
        final Mailer.DescriptorImpl descriptor = (Mailer.DescriptorImpl) jenkins.getDescriptor(Mailer.class);
        assertEquals("4441", descriptor.getSmtpPort());
        assertEquals("do-not-reply@acme.org", descriptor.getReplyToAddress());

        // FIXME setAdminAddress is deprecated and should NOT be set this way
        // see https://github.com/jenkinsci/mailer-plugin/pull/39
        assertEquals("admin@acme.org", descriptor.getAdminAddress());
    }
}
