package io.jenkins.plugins.casc.core;

import static io.jenkins.plugins.casc.ConfigurationAsCode.get;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.cloudbees.hudson.plugins.folder.Folder;
import com.cloudbees.hudson.plugins.folder.properties.FolderCredentialsProvider.FolderCredentialsProperty;
import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.domains.DomainCredentials;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import java.util.List;
import java.util.Objects;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;

public class FolderItemConfiguratorTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("folder-job.yaml")
    public void shouldCreateFolder() {
        Folder folder = Jenkins.get().getItemByFullName("my-folder", Folder.class);

        assertNotNull("Folder should have been created by JCasC", folder);
        assertEquals("My Folder", folder.getDisplayName());
        assertEquals("Configured via JCasC", folder.getDescription());
    }

    @Test
    public void shouldUpdateExistingFolder() throws Exception {
        Folder existing = j.jenkins.createProject(Folder.class, "existing-folder");
        existing.setDisplayName("Old Folder");
        existing.setDescription("Old Description");
        existing.save();

        get().configure(Objects.requireNonNull(getClass().getResource("folder-job-update.yaml"))
                .toExternalForm());

        Folder updated = Jenkins.get().getItemByFullName("existing-folder", Folder.class);
        assertNotNull(updated);
        assertEquals("Updated Folder", updated.getDisplayName());
        assertEquals("Updated via JCasC", updated.getDescription());
    }

    @Test
    @ConfiguredWithCode("folder-job.yaml")
    public void shouldRoundTripFolderExport() throws Exception {
        Folder folder = Jenkins.get().getItemByFullName("my-folder", Folder.class);
        assertNotNull(folder);

        FolderItemConfigurator configurator = new FolderItemConfigurator();
        ConfigurationContext context = new ConfigurationContext(ConfiguratorRegistry.get());

        CNode node = configurator.describe(folder, context);
        assertNotNull("Configurator should describe the folder", node);

        Mapping mapping = node.asMapping();
        assertEquals("my-folder", mapping.getScalarValue("name"));
        assertEquals("My Folder", mapping.getScalarValue("displayName"));
        assertEquals("Configured via JCasC", mapping.getScalarValue("description"));
    }

    @Test
    @ConfiguredWithCode("folder-with-credentials.yaml")
    public void shouldConfigureFolderCredentials() {
        Folder folder = Jenkins.get().getItemByFullName("secure-folder", Folder.class);
        assertNotNull("Folder should have been created", folder);

        FolderCredentialsProperty credentialsProperty = folder.getProperties().get(FolderCredentialsProperty.class);
        assertNotNull("FolderCredentialsProperty should be configured on the folder", credentialsProperty);

        List<DomainCredentials> domainCredentials = credentialsProperty.getDomainCredentials();
        assertEquals("Should contain one domain credential block", 1, domainCredentials.size());

        List<Credentials> credentialsList = domainCredentials.get(0).getCredentials();
        assertEquals("Should contain one credential", 1, credentialsList.size());

        Credentials credential = credentialsList.get(0);
        assertTrue(
                "Credential should be a Username/Password instance",
                credential instanceof UsernamePasswordCredentialsImpl);

        UsernamePasswordCredentialsImpl upCred = (UsernamePasswordCredentialsImpl) credential;
        assertEquals("folder-secret-id", upCred.getId());
        assertEquals("my-user", upCred.getUsername());
        assertEquals("A secret for this folder", upCred.getDescription());
    }

    @Test
    @ConfiguredWithCode("folder-with-credentials.yaml")
    public void shouldRoundTripFolderCredentialsExport() throws Exception {
        Folder folder = Jenkins.get().getItemByFullName("secure-folder", Folder.class);
        assertNotNull(folder);

        FolderItemConfigurator configurator = new FolderItemConfigurator();
        ConfigurationContext context = new ConfigurationContext(ConfiguratorRegistry.get());

        CNode node = configurator.describe(folder, context);
        assertNotNull(node);

        Mapping mapping = node.asMapping();
        assertEquals("secure-folder", mapping.getScalarValue("name"));

        CNode propertiesNode = mapping.get("properties");
        assertNotNull(propertiesNode);

        Mapping credsPropertyMapping = null;
        for (CNode propNode : propertiesNode.asSequence()) {
            Mapping propMapping = propNode.asMapping();
            if (propMapping.containsKey("folderCredentialsProperty")) {
                credsPropertyMapping =
                        propMapping.get("folderCredentialsProperty").asMapping();
                break;
            }
        }
        assertNotNull("folderCredentialsProperty should be exported", credsPropertyMapping);

        CNode domainCredsNode = credsPropertyMapping.get("domainCredentials");
        assertNotNull("domainCredentials should be exported", domainCredsNode);

        Mapping firstDomainCred = domainCredsNode.asSequence().get(0).asMapping();
        CNode credentialsNode = firstDomainCred.get("credentials");
        assertNotNull(credentialsNode);

        Mapping firstCredential = credentialsNode
                .asSequence()
                .get(0)
                .asMapping()
                .get("usernamePassword")
                .asMapping();
        assertEquals("folder-secret-id", firstCredential.getScalarValue("id"));
        assertEquals("my-user", firstCredential.getScalarValue("username"));
        assertEquals("A secret for this folder", firstCredential.getScalarValue("description"));
    }
}
