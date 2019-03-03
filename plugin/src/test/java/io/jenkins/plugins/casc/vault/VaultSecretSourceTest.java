package io.jenkins.plugins.casc.vault;


import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.SecretSourceResolver;
import org.junit.*;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.jvnet.hudson.test.JenkinsRule;
import org.testcontainers.vault.VaultContainer;

import static org.hamcrest.CoreMatchers.equalTo;
import static io.jenkins.plugins.casc.vault.VaultTestUtil.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;

// Inspired by https://github.com/BetterCloud/vault-java-driver/blob/master/src/test-integration/java/com/bettercloud/vault/util/VaultContainer.java
public class VaultSecretSourceTest {

    @ClassRule
    public static VaultContainer vaultContainer = createVaultContainer();

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Rule
    public EnvironmentVariables envVars = new EnvironmentVariables()
        .set("CASC_VAULT_FILE", getClass().getResource("vaultTest_cascFile").getPath());

    private ConfigurationContext context;

    @BeforeClass
    public static void configureContainer() {
        // Dont run on non-docker daemon nodes
        assumeTrue(hasDockerDaemon());

        // Create vault policies/users/roles ..
        configureVaultContainer(vaultContainer);
    }

    @Before
    public void refreshConfigurationContext() {
        // Setup Jenkins
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        context = new ConfigurationContext(registry);
    }

    @Test
    public void kv1WithUser() {
        envVars.set("CASC_VAULT_USER", VAULT_USER);
        envVars.set("CASC_VAULT_PW", VAULT_PW);
        envVars.set("CASC_VAULT_PATH", VAULT_PATH_V1);
        envVars.set("CASC_VAULT_ENGINE_VERSION", "1");
        assertThat(SecretSourceResolver.resolve(context, "${key1}"), equalTo("123"));
    }

    @Test
    public void kv2WithUser() {
        envVars.set("CASC_VAULT_USER", VAULT_USER);
        envVars.set("CASC_VAULT_PW", VAULT_PW);
        envVars.set("CASC_VAULT_PATH", VAULT_PATH_V2);
        envVars.set("CASC_VAULT_ENGINE_VERSION", "2");
        assertThat(SecretSourceResolver.resolve(context, "${key1}"), equalTo("123"));
    }

    @Test
    public void kv1WithToken() {
        envVars.set("CASC_VAULT_TOKEN", VAULT_ROOT_TOKEN);
        envVars.set("CASC_VAULT_PATH", VAULT_PATH_V1);
        envVars.set("CASC_VAULT_ENGINE_VERSION", "1");
        assertThat(SecretSourceResolver.resolve(context, "${key1}"), equalTo("123"));
    }

    @Test
    public void kv2WithToken() {
        envVars.set("CASC_VAULT_TOKEN", VAULT_ROOT_TOKEN);
        envVars.set("CASC_VAULT_PATH", VAULT_PATH_V2);
        envVars.set("CASC_VAULT_ENGINE_VERSION", "2");
        assertThat(SecretSourceResolver.resolve(context, "${key1}"), equalTo("123"));
    }

    @Test
    public void kv1WithApprole() {
        envVars.set("CASC_VAULT_APPROLE", VAULT_APPROLE_ID);
        envVars.set("CASC_VAULT_APPROLE_SECRET", VAULT_APPROLE_SECRET);
        envVars.set("CASC_VAULT_PATH", VAULT_PATH_V1);
        envVars.set("CASC_VAULT_ENGINE_VERSION", "1");
        assertThat(SecretSourceResolver.resolve(context, "${key1}"), equalTo("123"));
    }

    @Test
    public void kv2WithApprole() {
        envVars.set("CASC_VAULT_APPROLE", VAULT_APPROLE_ID);
        envVars.set("CASC_VAULT_APPROLE_SECRET", VAULT_APPROLE_SECRET);
        envVars.set("CASC_VAULT_PATH", VAULT_PATH_V2);
        envVars.set("CASC_VAULT_ENGINE_VERSION", "2");
        assertThat(SecretSourceResolver.resolve(context, "${key1}"), equalTo("123"));
    }
}
