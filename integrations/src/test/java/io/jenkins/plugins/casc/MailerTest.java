package io.jenkins.plugins.casc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import hudson.tasks.Mailer;
import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.Env;
import io.jenkins.plugins.casc.misc.EnvVarsRule;
import io.jenkins.plugins.casc.misc.Envs;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import java.util.Objects;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class MailerTest {

    public JenkinsConfiguredWithReadmeRule j = new JenkinsConfiguredWithReadmeRule();

    public EnvVarsRule environment = new EnvVarsRule();

    @Rule
    public RuleChain chain = RuleChain.outerRule(environment).around(j);

    @Test
    @Envs({@Env(name = "SMTP_PASSWORD", value = "super_secret_smtp_password")})
    @ConfiguredWithReadme("mailer/README.md")
    public void configure_mailer() {
        final Jenkins jenkins = Jenkins.get();
        final Mailer.DescriptorImpl descriptor = (Mailer.DescriptorImpl) jenkins.getDescriptor(Mailer.class);
        assertEquals("4441", Objects.requireNonNull(descriptor).getSmtpPort());
        assertEquals("do-not-reply@acme.org", descriptor.getReplyToAddress());
        assertEquals("smtp.acme.org", descriptor.getSmtpHost());
        assertEquals(
                "smtp_user",
                Objects.requireNonNull(descriptor.getAuthentication()).getUsername());
        assertNotNull(descriptor.getAuthentication().getPassword());
        assertEquals(
                "super_secret_smtp_password",
                descriptor.getAuthentication().getPassword().getPlainText());
    }
}
