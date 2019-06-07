package io.jenkins.plugins.casc;

import hudson.tasks.Mailer;
import io.jenkins.plugins.casc.misc.RoundTripAbstractTest;
import jenkins.model.Jenkins;
import org.jvnet.hudson.test.RestartableJenkinsRule;

import static org.junit.Assert.assertEquals;

public class RoundTripMailerTest extends RoundTripAbstractTest {
    @Override
    protected void assertConfiguredAsExpected(RestartableJenkinsRule j, String configContent) {
        final Jenkins jenkins = Jenkins.get();
        final Mailer.DescriptorImpl descriptor = (Mailer.DescriptorImpl) jenkins.getDescriptor(Mailer.class);
        assertEquals("4441", descriptor.getSmtpPort());
        assertEquals("do-not-reply@acme.org", descriptor.getReplyToAddress());

        // FIXME setAdminAddress is deprecated and should NOT be set this way
        // see https://github.com/jenkinsci/mailer-plugin/pull/39
        assertEquals("admin@acme.org", descriptor.getAdminAddress());
    }

    @Override
    protected String configResource() {
        return "MailerTest.yml";
    }

    @Override
    protected String stringInLogExpected() {
        return "do-not-reply@acme.org";
    }
}
