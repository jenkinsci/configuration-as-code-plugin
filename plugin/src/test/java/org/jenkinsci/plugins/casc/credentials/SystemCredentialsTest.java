/*
 * Copyright (c) 2018 CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to
 * whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkinsci.plugins.casc.credentials;

import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.CertificateCredentials;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.casc.Configurator;
import org.jenkinsci.plugins.casc.DataBoundConfigurator;
import org.jenkinsci.plugins.casc.misc.ConfiguredWithCode;
import org.jenkinsci.plugins.casc.misc.EnvVarsRule;
import org.jenkinsci.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.jenkinsci.plugins.casc.model.CNode;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.jvnet.hudson.test.LoggerRule;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class SystemCredentialsTest {

    @Rule
    public LoggerRule log = new LoggerRule()
        .recordPackage(DataBoundConfigurator.class, Level.INFO)
        .capture(100);

    @Rule
    public RuleChain chain = RuleChain.outerRule(new EnvVarsRule()
            .env("SUDO_PASSWORD", "1234")
            .env("SSH_PRIVATE_KEY", "s3cr3t")
            .env("SSH_KEY_PASSWORD", "ABCD"))
            .around(log)
            .around(new JenkinsConfiguredWithCodeRule());

    @Test
    @ConfiguredWithCode("SystemCredentialsTest.yml")
    public void configure_system_credentials() throws Exception {
        Jenkins jenkins = Jenkins.getInstance();

        List<UsernamePasswordCredentials> ups = CredentialsProvider.lookupCredentials(
                UsernamePasswordCredentials.class, jenkins, ACL.SYSTEM, Collections.emptyList()
        );
        assertThat(ups, hasSize(1));
        final UsernamePasswordCredentials up = ups.get(0);
        assertThat(up.getPassword().getPlainText(), equalTo("1234"));
        final CNode node = Configurator.lookup(up.getClass()).describe(up);
        assertEquals("1234", node.asMapping().getScalarValue("password"));


        List<CertificateCredentials> certs = CredentialsProvider.lookupCredentials(
                CertificateCredentials.class, jenkins, ACL.SYSTEM, Collections.emptyList()
        );
        assertThat(certs, hasSize(1));
        assertThat(certs.get(0).getPassword().getPlainText(), equalTo("ABCD"));

        List<BasicSSHUserPrivateKey> sshPrivateKeys = CredentialsProvider.lookupCredentials(
                BasicSSHUserPrivateKey.class, jenkins, ACL.SYSTEM, Collections.emptyList()
        );
        assertThat(sshPrivateKeys, hasSize(2));
        assertThat(sshPrivateKeys.get(0).getPassphrase().getPlainText(), equalTo("ABCD"));

        // credentials should not appear in plain text in log
        for (LogRecord logRecord : log.getRecords()) {
            assertThat(logRecord.getMessage(), not(containsString("1234")));
            assertThat(logRecord.getMessage(), not(containsString("ABCD")));
        }


    }
}
