package org.jenkinsci.plugins.casc;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.SecretBytes;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.casc.misc.ConfiguredWithCode;
import org.jenkinsci.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.jenkinsci.plugins.plaincredentials.FileCredentials;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class PlainCredentialsTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("PlainCredentialsTest.yml")
    public void should_configure_credentials_file() throws IOException {
        Jenkins jenkins = Jenkins.getInstance();

        FileCredentials c = CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(
                        FileCredentials.class, j.jenkins, ACL.SYSTEM), CredentialsMatchers.withId("secret-file"));
        assertNotNull(c);
        assertEquals("my-secret-file", c.getFileName());
        assertEquals("FOO_BAR", IOUtils.toString(c.getContent()));

    }
}
