package io.jenkins.plugins.casc;

import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import hudson.ExtensionList;
import hudson.util.Secret;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import io.jenkins.plugins.casc.support.credentials.CredentialsRootConfigurator;
import java.util.Collections;
import java.util.List;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;

import static java.util.Objects.requireNonNull;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class CredentialsTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @ConfiguredWithCode("GlobalCredentials.yml")
    @Test
    public void testGlobalScopedCredentials() {
        List<StandardUsernamePasswordCredentials> creds = CredentialsProvider.lookupCredentials(StandardUsernamePasswordCredentials.class,Jenkins.getInstanceOrNull(), null, Collections.emptyList());
        assertThat(creds.size(), is(1));
        assertEquals("user1", creds.get(0).getId());
        assertEquals("Administrator", creds.get(0).getUsername());
        assertEquals("secretPassword", creds.get(0).getPassword().getPlainText());

        List<BasicSSHUserPrivateKey> creds2 = CredentialsProvider.lookupCredentials(BasicSSHUserPrivateKey.class,Jenkins.getInstanceOrNull(), null, Collections.emptyList());
        assertThat(creds2.size(), is(1));
        BasicSSHUserPrivateKey basicSSHUserPrivateKey = creds2.get(0);
        assertEquals("agentuser", basicSSHUserPrivateKey.getUsername());
        assertEquals("password", basicSSHUserPrivateKey.getPassphrase().getPlainText());
        assertEquals("ssh private key used to connect ssh slaves", basicSSHUserPrivateKey.getDescription());
        assertThat(basicSSHUserPrivateKey.getPrivateKeySource().getPrivateKeys().size(), is(1));
        String directKey = basicSSHUserPrivateKey.getPrivateKeySource().getPrivateKeys().get(0);
        assertThat(directKey, is("sp0ds9d+skkfjf"));

    }


    @ConfiguredWithCode("CredentialsWithDomain.yml")
    @Test
    public void testDomainScopedCredentials() {
        List<StandardUsernamePasswordCredentials> creds = CredentialsProvider.lookupCredentials(StandardUsernamePasswordCredentials.class,Jenkins.getInstanceOrNull(), null, Collections.emptyList());
        assertThat(creds.size(), is(1));
        assertEquals("user1", creds.get(0).getId());
        assertEquals("Administrator", creds.get(0).getUsername());
        assertEquals("secret", creds.get(0).getPassword().getPlainText());
    }

    @ConfiguredWithCode("GlobalCredentials.yml")
    @Test
    public void testExportSSHCredentials() throws Exception {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        CredentialsRootConfigurator root = ExtensionList.lookupSingleton(CredentialsRootConfigurator.class);

        CNode node = root.describe(root.getTargetComponent(context), context);
        assertNotNull(node);
        final Mapping mapping = node.asMapping();

        Mapping sshCredential = mapping.get("system")
                .asMapping()
                .get("domainCredentials")
                .asSequence().get(0)
                .asMapping().get("credentials")
                .asSequence().get(1)
                .asMapping().get("basicSSHUserPrivateKey").asMapping();

        assertThat(sshCredential.getScalarValue("scope"), is("SYSTEM"));
        assertThat(sshCredential.getScalarValue("id"), is("agent-private-key"));
        assertThat(sshCredential.getScalarValue("username"), is("agentuser"));

        String passphrase = sshCredential.getScalarValue("passphrase");
        assertThat(passphrase, not("password"));
        assertThat(requireNonNull(Secret.decrypt(passphrase)).getPlainText(), is("password"));

        String sshKeyExported = sshCredential.get("privateKeySource")
                .asMapping()
                .get("directEntry")
                .asMapping()
                .get("privateKey")
                .asScalar()
                .getValue();

        assertThat(sshKeyExported, not("sp0ds9d+skkfjf"));
        assertThat(requireNonNull(Secret.decrypt(sshKeyExported)).getPlainText(), is("sp0ds9d+skkfjf"));
    }

}
