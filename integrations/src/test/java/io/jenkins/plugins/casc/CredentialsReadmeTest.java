package io.jenkins.plugins.casc;

import com.cloudbees.jenkins.plugins.awscredentials.AWSCredentialsImpl;
import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey;
import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SecretBytes;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.impl.CertificateCredentialsImpl;
import com.cloudbees.plugins.credentials.impl.CertificateCredentialsImpl.UploadedKeyStoreSource;
import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.Env;
import io.jenkins.plugins.casc.misc.EnvVarsRule;
import io.jenkins.plugins.casc.misc.Envs;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import jenkins.model.Jenkins;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.plaincredentials.FileCredentials;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.jvnet.hudson.test.JenkinsMatchers.hasPlainText;

public class CredentialsReadmeTest {

    public static final String PASSPHRASE = "passphrase";
    public static final String PRIVATE_KEY = "-----BEGIN RSA PRIVATE KEY-----\n"
        + "MIICXgIBAAKBgQC2xOoDBS+RQiwYN+rY0YXYQ/WwmC9ICH7BCXfLUBSHAkF2Dvd0\n"
        + "cvM2Ph2nOPiHdntrvD8JkzIv+S1RIqlBrzK6NRQ0ojoCvyawzY3cgzfQ4dfaOqUF\n"
        + "2bn4PscioLlq+Pbi3KYYwWUFue2iagRIxp0+/3F5WqOWPPy1twW7ddWPLQIDAQAB\n"
        + "AoGBAKOX7DKZ4LLvfRKMcpxyJpCme/L+tUuPtw1IUT7dxhH2deubh+lmvsXtoZM9\n"
        + "jk+KQPz0+aOzanfAXMzD7qZJkGfQ91aG8OiD+YJnRqOc6C6vQBXiZgeHRqWH0VMG\n"
        + "rp9Xqd8MxEYScaJYMwoHiBCG/cb3c4kpEpZ03IzkekZdXlmhAkEA7iFEk5k1BZ1+\n"
        + "BnKN9LxLR0EOKoSFJjxBihRP6+UD9BF+/1AlKlLW4hSq4458ppV5Wt4glHTcAQi/\n"
        + "U+wOOz6DyQJBAMR8G0yjtmLjMBy870GaDxmwWjqSeYwPoHbvRTOml8Fz9fP4gBMi\n"
        + "PUEGJaEHMuPECIegZ93kwAGBT51Q7AZcukUCQGGmnNOWISsjUXndYh85U/ltURzY\n"
        + "aS2rygiQmdGXgY6F2jliqUr424ushAN6+9zoMPK1YlDetxVpe+QzSga7dRkCQQCB\n"
        + "+DI6rORdXziZGeUNuPGaJYxZyEA8hK25Xqag9ubVYXZlLpDRl0l7dKx5awCfpzGZ\n"
        + "PWLXZZQYqsfWIQwvXTEdAkEA2bziyReYAb9fi17alcvwZXGzyyMY8WOGns8NZKcq\n"
        + "INF8D3PDpcCyOvQI/TS3qHYmGyWdHiKCWsgBqE6kyjqpNQ==\n"
        + "-----END RSA PRIVATE KEY-----\n";
    public static final String PASSWORD = "password";
    public static final String TEXT = "text";
    public static final String ACCESS_KEY = "access-key";
    public static final String SECRET_ACCESS_KEY = "secret-access-key";
    public static final String MYSECRETFILE_TXT = "mysecretfile.txt";
    public static final String TEST_CERT = "test.p12";
    public JenkinsConfiguredWithReadmeRule j = new JenkinsConfiguredWithReadmeRule();

    public EnvVarsRule environment = new EnvVarsRule();

    @Rule
    public RuleChain chain = RuleChain
        .outerRule(environment)
        .around(j);

    @Test
    @ConfiguredWithReadme("credentials/README.md#0")
    @Envs({
        @Env(name = "SUDO_PASSWORD", value = "SUDO")
    })
    public void testDomainScopedCredentials() {
        List<StandardUsernamePasswordCredentials> creds = CredentialsProvider
            .lookupCredentials(StandardUsernamePasswordCredentials.class,
                Jenkins.getInstanceOrNull(), null, Collections.emptyList());
        assertThat(creds.size(), is(1));
        StandardUsernamePasswordCredentials cred = creds.get(0);
        assertThat(cred.getId(), is("sudo_password"));
        assertThat(cred.getUsername(), is("root"));
        assertThat(cred.getPassword(), hasPlainText("SUDO"));
    }

    @Test
    @ConfiguredWithReadme("credentials/README.md#1")
    @Envs({
        @Env(name = "SSH_KEY_PASSWORD", value = PASSPHRASE),
        @Env(name = "SSH_PRIVATE_KEY", value = PRIVATE_KEY),
        @Env(name = "SSH_PRIVATE_FILE_PATH", value = "private-key.pem"),
        @Env(name = "SOME_USER_PASSWORD", value = PASSWORD),
        @Env(name = "SECRET_TEXT", value = TEXT),
        @Env(name = "AWS_ACCESS_KEY", value = ACCESS_KEY),
        @Env(name = "AWS_SECRET_ACCESS_KEY", value = SECRET_ACCESS_KEY),
        @Env(name = "SECRET_FILE_PATH", value = MYSECRETFILE_TXT),
        @Env(name = "SECRET_PASSWORD_CERT", value = PASSWORD),
        @Env(name = "SECRET_CERT_FILE_PATH", value = TEST_CERT),
    })
    public void testGlobalScopedCredentials() throws Exception {
        List<Credentials> creds = CredentialsProvider.lookupCredentials(
            Credentials.class, Jenkins.get(), null, Collections.emptyList());
        assertThat(creds, hasSize(8));
        for (Credentials credentials : creds) {
            if (credentials instanceof BasicSSHUserPrivateKey) {
                BasicSSHUserPrivateKey key = (BasicSSHUserPrivateKey) credentials;
                assertThat(key.getPassphrase(), hasPlainText(PASSPHRASE));
                assertThat(key.getPrivateKey(), equalTo(PRIVATE_KEY));
                assertThat(key.getId(), anyOf(
                    is("ssh_with_passphrase_provided"),
                    is("ssh_with_passphrase_provided_via_file")));
                assertThat(key.getUsername(), is("ssh_root"));
                assertThat(key.getScope(), is(CredentialsScope.SYSTEM));
            } else if (credentials instanceof UsernamePasswordCredentials) {
                UsernamePasswordCredentials user = (UsernamePasswordCredentials) credentials;
                assertThat(user.getUsername(), is("some-user"));
                assertThat(user.getPassword(), hasPlainText(PASSWORD));
                assertThat(user.getScope(), is(CredentialsScope.GLOBAL));
            } else if (credentials instanceof StringCredentials) {
                StringCredentials string = (StringCredentials) credentials;
                assertThat(string.getId(), is("secret-text"));
                assertThat(string.getSecret(), hasPlainText(TEXT));
                assertThat(string.getScope(), is(CredentialsScope.GLOBAL));
            } else if (credentials instanceof AWSCredentialsImpl) {
                AWSCredentialsImpl aws = (AWSCredentialsImpl) credentials;
                assertThat(aws.getId(), is("AWS"));
                assertThat(aws.getAccessKey(), equalTo(ACCESS_KEY));
                assertThat(aws.getSecretKey(), hasPlainText(SECRET_ACCESS_KEY));
                assertThat(aws.getScope(), is(CredentialsScope.GLOBAL));
            } else if (credentials instanceof FileCredentials) {
                FileCredentials file = (FileCredentials) credentials;
                assertThat(file.getId(), anyOf(is("secret-file"), is("secret-file_via_binary_file")));
                assertThat(file.getFileName(), is(MYSECRETFILE_TXT));
                String fileContent = IOUtils.toString(file.getContent(), StandardCharsets.UTF_8);
                assertThat(fileContent, containsString("SUPER SECRET"));
                assertThat(file.getScope(), is(CredentialsScope.GLOBAL));
            } else if (credentials instanceof CertificateCredentialsImpl) {
                CertificateCredentialsImpl cert = (CertificateCredentialsImpl) credentials;
                assertThat(cert.getId(), is("secret-certificate"));
                assertThat(cert.getPassword(), hasPlainText(PASSWORD));
                byte[] fileContent = Files.readAllBytes(Paths.get(getClass().getResource(TEST_CERT).toURI()));
                SecretBytes secretBytes = SecretBytes
                    .fromString(Base64.getEncoder().encodeToString(fileContent));
                UploadedKeyStoreSource keyStoreSource = (UploadedKeyStoreSource) cert.getKeyStoreSource();
                assertThat(keyStoreSource.getUploadedKeystore().getPlainData(),
                        is(secretBytes.getPlainData()));
                assertThat(cert.getKeyStore().containsAlias("1"), is(true));
                assertThat(cert.getKeyStore().getCertificate("1").getType(), is("X.509"));
                assertThat(CredentialsNameProvider.name(cert), is("EMAILADDRESS=me@myhost.mydomain, CN=pkcs12, O=Fort-Funston, L=SanFrancisco, ST=CA, C=US (my secret cert)"));
                assertThat(cert.getScope(), is(CredentialsScope.GLOBAL));
            }
        }
    }

}
