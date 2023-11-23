package io.jenkins.plugins.casc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

import hudson.tasks.Mailer;
import io.jenkins.plugins.casc.misc.RoundTripAbstractTest;
import jenkins.model.Jenkins;
import org.junit.Before;
import org.jvnet.hudson.test.RestartableJenkinsRule;

public class RoundTripMailerTest extends RoundTripAbstractTest {
    @Before
    public void shouldThisRun() {
        assumeTrue(ShouldRun.thisTest());
    }

    @Override
    protected void assertConfiguredAsExpected(RestartableJenkinsRule j, String configContent) {
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
