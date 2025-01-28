package io.jenkins.plugins.casc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import hudson.tasks.Mailer;
import io.jenkins.plugins.casc.misc.junit.jupiter.AbstractRoundTripTest;
import jenkins.model.Jenkins;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class RoundTripMailerTest extends AbstractRoundTripTest {
    @Override
    protected void assertConfiguredAsExpected(JenkinsRule j, String configContent) {
        final Jenkins jenkins = Jenkins.get();
        final Mailer.DescriptorImpl descriptor = (Mailer.DescriptorImpl) jenkins.getDescriptor(Mailer.class);
        assertEquals("4441", descriptor.getSmtpPort());
        assertEquals("do-not-reply@acme.org", descriptor.getReplyToAddress());
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
