package org.jenkinsci.plugins.casc;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.CertificateCredentials;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import hudson.security.ACL;
import org.jenkinsci.plugins.casc.misc.CodeConfiguratorRunner;
import org.jenkinsci.plugins.casc.misc.ConfiguredWithCode;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.rules.RuleChain;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class SystemCredentialsTest {

    public JenkinsRule j = new JenkinsRule();
    public CodeConfiguratorRunner config = new CodeConfiguratorRunner();

    @Rule
    public RuleChain chain = RuleChain.outerRule(j)
            .around(new ExternalResource() {
                @Override
                protected void before() {
                    System.setProperty("SUDO_PASSWORD", "1234");
                    System.setProperty("SSH_KEY_PASSWORD", "ABCD");
                }
            })
            .around(config);

    @Test
    @ConfiguredWithCode("SystemCredentialsTest.yml")
    public void configure_system_credentials() throws Exception {
        List<UsernamePasswordCredentials> ups = CredentialsProvider.lookupCredentials(UsernamePasswordCredentials.class, j.jenkins, ACL.SYSTEM, Collections.emptyList());
        assertEquals(1, ups.size());
        final UsernamePasswordCredentials up = ups.get(0);
        assertEquals("1234", up.getPassword().getPlainText());

        List<CertificateCredentials> certs = CredentialsProvider.lookupCredentials(CertificateCredentials.class, j.jenkins, ACL.SYSTEM, Collections.emptyList());
        assertEquals(1, certs.size());
        final CertificateCredentials cert = certs.get(0);
        assertEquals("ABCD", cert.getPassword().getPlainText());

    }
}
