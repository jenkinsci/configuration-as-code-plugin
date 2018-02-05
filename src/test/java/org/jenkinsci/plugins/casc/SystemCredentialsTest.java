package org.jenkinsci.plugins.casc;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.CertificateCredentials;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import hudson.security.ACL;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class SystemCredentialsTest {


    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void configure_system_credentials() throws Exception {
        System.setProperty("SUDO_PASSWORD", "1234");
        System.setProperty("SSH_KEY_PASSWORD", "ABCD");

        ConfigurationAsCode.configure(getClass().getResourceAsStream("SystemCredentialsTest.yml"));

        List<UsernamePasswordCredentials> ups = CredentialsProvider.lookupCredentials(UsernamePasswordCredentials.class, j.jenkins, ACL.SYSTEM, Collections.EMPTY_LIST);
        assertEquals(1, ups.size());
        final UsernamePasswordCredentials up = ups.get(0);
        assertEquals("1234", up.getPassword().getPlainText());

        List<CertificateCredentials> certs = CredentialsProvider.lookupCredentials(CertificateCredentials.class, j.jenkins, ACL.SYSTEM, Collections.EMPTY_LIST);
        assertEquals(1, certs.size());
        final CertificateCredentials cert = certs.get(0);
        assertEquals("ABCD", cert.getPassword().getPlainText());

    }
}
