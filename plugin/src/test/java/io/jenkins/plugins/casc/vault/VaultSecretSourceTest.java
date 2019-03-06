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
        // Check if docker daemon is available
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
        envVars.set("CASC_VAULT_PATHS", VAULT_PATH_KV1_1 + "," + VAULT_PATH_KV1_2);
        envVars.set("CASC_VAULT_ENGINE_VERSION", "1");
        assertThat(SecretSourceResolver.resolve(context, "${key1}"), equalTo("123"));
    }

    @Test
    public void kv2WithUser() {
        envVars.set("CASC_VAULT_USER", VAULT_USER);
        envVars.set("CASC_VAULT_PW", VAULT_PW);
        envVars.set("CASC_VAULT_PATHS", VAULT_PATH_KV2_1 + "," + VAULT_PATH_KV2_2);
        envVars.set("CASC_VAULT_ENGINE_VERSION", "2");
        assertThat(SecretSourceResolver.resolve(context, "${key1}"), equalTo("123"));
    }

    @Test
    public void kv2WithWrongUser() {
        envVars.set("CASC_VAULT_USER", "1234");
        envVars.set("CASC_VAULT_PW", VAULT_PW);
        envVars.set("CASC_VAULT_PATHS", VAULT_PATH_KV2_1);
        envVars.set("CASC_VAULT_ENGINE_VERSION", "2");
        assertThat(SecretSourceResolver.resolve(context, "${key1}"), equalTo(""));
    }

    @Test
    public void kv1WithToken() {
        envVars.set("CASC_VAULT_TOKEN", VAULT_ROOT_TOKEN);
        envVars.set("CASC_VAULT_PATHS", VAULT_PATH_KV1_1 + "," + VAULT_PATH_KV1_2);
        envVars.set("CASC_VAULT_ENGINE_VERSION", "1");
        assertThat(SecretSourceResolver.resolve(context, "${key1}"), equalTo("123"));
    }

    @Test
    public void kv2WithToken() {
        envVars.set("CASC_VAULT_TOKEN", VAULT_ROOT_TOKEN);
        envVars.set("CASC_VAULT_PATHS", VAULT_PATH_KV2_1 + "," + VAULT_PATH_KV2_2);
        envVars.set("CASC_VAULT_ENGINE_VERSION", "2");
        assertThat(SecretSourceResolver.resolve(context, "${key1}"), equalTo("123"));
    }

    @Test
    public void kv1WithWrongToken() {
        envVars.set("CASC_VAULT_TOKEN", "1234");
        envVars.set("CASC_VAULT_PATHS", VAULT_PATH_KV1_1);
        envVars.set("CASC_VAULT_ENGINE_VERSION", "1");
        assertThat(SecretSourceResolver.resolve(context, "${key1}"), equalTo(""));
    }

    @Test
    public void kv1WithApprole() {
        envVars.set("CASC_VAULT_APPROLE", VAULT_APPROLE_ID);
        envVars.set("CASC_VAULT_APPROLE_SECRET", VAULT_APPROLE_SECRET);
        envVars.set("CASC_VAULT_PATHS", VAULT_PATH_KV1_1 + "," + VAULT_PATH_KV1_2);
        envVars.set("CASC_VAULT_ENGINE_VERSION", "1");
        assertThat(SecretSourceResolver.resolve(context, "${key1}"), equalTo("123"));
    }

    @Test
    public void kv2WithApprole() {
        envVars.set("CASC_VAULT_APPROLE", VAULT_APPROLE_ID);
        envVars.set("CASC_VAULT_APPROLE_SECRET", VAULT_APPROLE_SECRET);
        envVars.set("CASC_VAULT_PATHS", VAULT_PATH_KV2_1 + "," + VAULT_PATH_KV2_2);
        envVars.set("CASC_VAULT_ENGINE_VERSION", "2");
        assertThat(SecretSourceResolver.resolve(context, "${key1}"), equalTo("123"));
    }

    @Test
    public void kv2WithWrongApprole() {
        envVars.set("CASC_VAULT_APPROLE", "1234");
        envVars.set("CASC_VAULT_APPROLE_SECRET", VAULT_APPROLE_SECRET);
        envVars.set("CASC_VAULT_PATHS", VAULT_PATH_KV2_1);
        envVars.set("CASC_VAULT_ENGINE_VERSION", "2");
        assertThat(SecretSourceResolver.resolve(context, "${key1}"), equalTo(""));
    }

    @Test
    public void kv2WithApproleMultipleKeys() {
        envVars.set("CASC_VAULT_APPROLE", VAULT_APPROLE_ID);
        envVars.set("CASC_VAULT_APPROLE_SECRET", VAULT_APPROLE_SECRET);
        envVars.set("CASC_VAULT_PATHS", VAULT_PATH_KV2_1 + "," + VAULT_PATH_KV2_2);
        envVars.set("CASC_VAULT_ENGINE_VERSION", "2");
        assertThat(SecretSourceResolver.resolve(context, "${key2}"), equalTo("456"));
        assertThat(SecretSourceResolver.resolve(context, "${key3}"), equalTo("789"));
    }

    @Test
    public void kv2WithApproleMultipleKeysOverriden() {
        envVars.set("CASC_VAULT_APPROLE", VAULT_APPROLE_ID);
        envVars.set("CASC_VAULT_APPROLE_SECRET", VAULT_APPROLE_SECRET);
        envVars.set("CASC_VAULT_PATHS", VAULT_PATH_KV2_1 + "," + VAULT_PATH_KV2_2 + "," + VAULT_PATH_KV2_3);
        envVars.set("CASC_VAULT_ENGINE_VERSION", "2");
        assertThat(SecretSourceResolver.resolve(context, "${key2}"), equalTo("321"));
        assertThat(SecretSourceResolver.resolve(context, "${key1}"), equalTo("123"));
    }

    // TODO: used to check for backwards compatibility. Deprecate!
    @Test
    public void kv2WithUserDeprecatedPath() {
        envVars.set("CASC_VAULT_APPROLE", VAULT_APPROLE_ID);
        envVars.set("CASC_VAULT_APPROLE_SECRET", VAULT_APPROLE_SECRET);
        envVars.set("CASC_VAULT_PATH", VAULT_PATH_KV1_1 + "," + VAULT_PATH_KV1_2);
        envVars.set("CASC_VAULT_ENGINE_VERSION", "1");
        assertThat(SecretSourceResolver.resolve(context, "${key3}"), equalTo("789"));
    }
}
