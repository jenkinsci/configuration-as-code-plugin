package org.jenkinsci.plugins.casc;

import hudson.tasks.Mailer;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.casc.misc.CodeConfiguratorRunner;
import org.jenkinsci.plugins.casc.misc.ConfiguredWithCode;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class MailerTest {

    public JenkinsRule j = new JenkinsRule();
    public CodeConfiguratorRunner config = new CodeConfiguratorRunner();

    @Rule
    public RuleChain chain = RuleChain.outerRule(j).around(config);


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
