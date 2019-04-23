package io.jenkins.plugins.casc.vault;


import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.SecretSource;
import io.jenkins.plugins.casc.SecretSourceResolver;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.Env;
import io.jenkins.plugins.casc.misc.EnvVarsRule;
import io.jenkins.plugins.casc.misc.Envs;
import io.jenkins.plugins.casc.misc.EnvsFromFile;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.testcontainers.vault.VaultContainer;

import static io.jenkins.plugins.casc.vault.VaultTestUtil.VAULT_APPROLE_FILE;
import static io.jenkins.plugins.casc.vault.VaultTestUtil.VAULT_PATH_KV1_1;
import static io.jenkins.plugins.casc.vault.VaultTestUtil.VAULT_PATH_KV1_2;
import static io.jenkins.plugins.casc.vault.VaultTestUtil.VAULT_PATH_KV2_1;
import static io.jenkins.plugins.casc.vault.VaultTestUtil.VAULT_PATH_KV2_2;
import static io.jenkins.plugins.casc.vault.VaultTestUtil.VAULT_PATH_KV2_3;
import static io.jenkins.plugins.casc.vault.VaultTestUtil.VAULT_PATH_KV2_AUTH_TEST;
import static io.jenkins.plugins.casc.vault.VaultTestUtil.VAULT_PW;
import static io.jenkins.plugins.casc.vault.VaultTestUtil.VAULT_ROOT_TOKEN;
import static io.jenkins.plugins.casc.vault.VaultTestUtil.VAULT_USER;
import static io.jenkins.plugins.casc.vault.VaultTestUtil.configureVaultContainer;
import static io.jenkins.plugins.casc.vault.VaultTestUtil.createVaultContainer;
import static io.jenkins.plugins.casc.vault.VaultTestUtil.hasDockerDaemon;
import static io.jenkins.plugins.casc.vault.VaultTestUtil.runCommand;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;

// Inspired by https://github.com/BetterCloud/vault-java-driver/blob/master/src/test-integration/java/com/bettercloud/vault/util/VaultContainer.java
public class VaultSecretSourceTest {

    private final static Logger LOGGER = Logger.getLogger(VaultSecretSourceTest.class.getName());

    @ClassRule
    public static VaultContainer vaultContainer = createVaultContainer();

    @Rule
    public RuleChain chain = RuleChain
        .outerRule(new EnvVarsRule()
            .set("CASC_VAULT_FILE", getClass().getResource("vaultTest_cascFile").getPath()))
        .around(new JenkinsConfiguredWithCodeRule());

    private ConfigurationContext context;

    @BeforeClass
    public static void configureContainer() {
        // Check if docker daemon is available
        assumeTrue(hasDockerDaemon());

        // Create vault policies/users/roles ..
        configureVaultContainer(vaultContainer);
    }

    @AfterClass
    public static void removeAppRoleFile() {
        File file = Paths.get(System.getProperty("java.io.tmpdir"), VAULT_APPROLE_FILE).toFile();
        assert file.delete() || !file.exists();
    }

    @Before
    public void refreshConfigurationContext() {
        // Setup Jenkins
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        context = new ConfigurationContext(registry);
    }

    @Test
    @ConfiguredWithCode("vault.yml")
    @Envs({
        @Env(name = "CASC_VAULT_USER", value = VAULT_USER),
        @Env(name = "CASC_VAULT_PW", value = VAULT_PW),
        @Env(name = "CASC_VAULT_PATHS", value = VAULT_PATH_KV1_1 + "," + VAULT_PATH_KV1_2),
        @Env(name = "CASC_VAULT_ENGINE_VERSION", value = "1")
    })
    public void kv1WithUser() {
        assertThat(SecretSourceResolver.resolve(context, "${key1}"), equalTo("123"));
    }

    @Test
    @ConfiguredWithCode("vault.yml")
    @Envs({
        @Env(name = "CASC_VAULT_USER", value = VAULT_USER),
        @Env(name = "CASC_VAULT_PW", value = VAULT_PW),
        @Env(name = "CASC_VAULT_PATHS", value = VAULT_PATH_KV2_1 + "," + VAULT_PATH_KV2_2),
        @Env(name = "CASC_VAULT_ENGINE_VERSION", value = "2")
    })
    public void kv2WithUser() {
        assertThat(SecretSourceResolver.resolve(context, "${key1}"), equalTo("123"));
    }

    @Test
    @ConfiguredWithCode("vault.yml")
    @Envs({
        @Env(name = "CASC_VAULT_USER", value = "1234"),
        @Env(name = "CASC_VAULT_PW", value = VAULT_PW),
        @Env(name = "CASC_VAULT_PATHS", value = VAULT_PATH_KV2_1),
        @Env(name = "CASC_VAULT_ENGINE_VERSION", value = "2")
    })
    public void kv2WithWrongUser() {
        assertThat(SecretSourceResolver.resolve(context, "${key1}"), equalTo(""));
    }

    @Test
    @ConfiguredWithCode("vault.yml")
    @Envs({
        @Env(name = "CASC_VAULT_TOKEN", value = VAULT_ROOT_TOKEN),
        @Env(name = "CASC_VAULT_PATHS", value = VAULT_PATH_KV1_1 + "," + VAULT_PATH_KV1_2),
        @Env(name = "CASC_VAULT_ENGINE_VERSION", value = "1")
    })
    public void kv1WithToken() {
        assertThat(SecretSourceResolver.resolve(context, "${key1}"), equalTo("123"));
    }

    @Test
    @ConfiguredWithCode("vault.yml")
    @Envs({
        @Env(name = "CASC_VAULT_TOKEN", value = VAULT_ROOT_TOKEN),
        @Env(name = "CASC_VAULT_PATHS", value = VAULT_PATH_KV2_1 + "," + VAULT_PATH_KV2_2),
        @Env(name = "CASC_VAULT_ENGINE_VERSION", value = "2")
    })
    public void kv2WithToken() {
        assertThat(SecretSourceResolver.resolve(context, "${key1}"), equalTo("123"));
    }

    @Test
    @ConfiguredWithCode("vault.yml")
    @Envs({
        @Env(name = "CASC_VAULT_TOKEN", value = "1234"),
        @Env(name = "CASC_VAULT_PATHS", value = VAULT_PATH_KV1_1),
        @Env(name = "CASC_VAULT_ENGINE_VERSION", value = "1")
    })
    public void kv1WithWrongToken() {
        assertThat(SecretSourceResolver.resolve(context, "${key1}"), equalTo(""));
    }

    @Test
    @ConfiguredWithCode("vault.yml")
    @EnvsFromFile(VAULT_APPROLE_FILE)
    @Envs({
        @Env(name = "CASC_VAULT_PATHS", value = VAULT_PATH_KV1_1 + "," + VAULT_PATH_KV1_2),
        @Env(name = "CASC_VAULT_ENGINE_VERSION", value = "1")
    })
    public void kv1WithApprole() {
        assertThat(SecretSourceResolver.resolve(context, "${key1}"), equalTo("123"));
    }

    @Test
    @ConfiguredWithCode("vault.yml")
    @EnvsFromFile(VAULT_APPROLE_FILE)
    @Envs({
        @Env(name = "CASC_VAULT_PATHS", value = VAULT_PATH_KV2_1 + "," + VAULT_PATH_KV2_2),
        @Env(name = "CASC_VAULT_ENGINE_VERSION", value = "2")
    })
    public void kv2WithApprole() throws ConfiguratorException {
        assertThat(SecretSourceResolver.resolve(context, "${key1}"), equalTo("123"));
    }

    @Test
    @ConfiguredWithCode("vault.yml")
    @EnvsFromFile(VAULT_APPROLE_FILE)
    @Envs({
        @Env(name = "CASC_VAULT_APPROLE", value = "1234"),
        @Env(name = "CASC_VAULT_PATHS", value = VAULT_PATH_KV2_1),
        @Env(name = "CASC_VAULT_ENGINE_VERSION", value = "2")
    })
    public void kv2WithWrongApprole() {
        assertThat(SecretSourceResolver.resolve(context, "${key1}"), equalTo(""));
    }

    @Test
    @ConfiguredWithCode("vault.yml")
    @EnvsFromFile(VAULT_APPROLE_FILE)
    @Envs({
        @Env(name = "CASC_VAULT_PATHS", value = VAULT_PATH_KV2_1 + "," + VAULT_PATH_KV2_2),
        @Env(name = "CASC_VAULT_ENGINE_VERSION", value = "2")
    })
    public void kv2WithApproleMultipleKeys() {
        assertThat(SecretSourceResolver.resolve(context, "${key2}"), equalTo("456"));
        assertThat(SecretSourceResolver.resolve(context, "${key3}"), equalTo("789"));
    }

    @Test
    @ConfiguredWithCode("vault.yml")
    @EnvsFromFile(VAULT_APPROLE_FILE)
    @Envs({
        @Env(name = "CASC_VAULT_PATHS", value = VAULT_PATH_KV2_1 + "," + VAULT_PATH_KV2_2 + "," + VAULT_PATH_KV2_3),
        @Env(name = "CASC_VAULT_ENGINE_VERSION", value = "2")
    })
    public void kv2WithApproleMultipleKeysOverriden() {
        assertThat(SecretSourceResolver.resolve(context, "${key2}"), equalTo("321"));
        assertThat(SecretSourceResolver.resolve(context, "${key1}"), equalTo("123"));
    }

    @Test
    @ConfiguredWithCode("vault.yml")
    @EnvsFromFile(VAULT_APPROLE_FILE)
    @Envs({
        @Env(name = "CASC_VAULT_PATHS", value = VAULT_PATH_KV2_AUTH_TEST),
        @Env(name = "CASC_VAULT_ENGINE_VERSION", value = "2")
    })
    public void kv2WithApproleWithReauth() throws Exception {
        assertThat(SecretSourceResolver.resolve(context, "${key1}"), equalTo("auth-test"));

        try {
            // Update secret
            runCommand(vaultContainer, "vault", "kv", "put", VAULT_PATH_KV2_AUTH_TEST,
                "key1=re-auth-test");
        } catch (InterruptedException e) {
            LOGGER.log(Level.WARNING, "Test got interrupted", e);
            assert false;
        } catch (IOException eio) {
            LOGGER.log(Level.WARNING, "Could not update vault secret for test", eio);
            assert false;
        }

        // SecretSource.init is normally called on configure
        context.getSecretSources().forEach(SecretSource::init);
        assertThat(SecretSourceResolver.resolve(context, "${key1}"), equalTo("re-auth-test"));
    }

    // TODO: used to check for backwards compatibility. Deprecate!
    @Test
    @ConfiguredWithCode("vault.yml")
    @EnvsFromFile(VAULT_APPROLE_FILE)
    @Envs({
        @Env(name = "CASC_VAULT_PATH", value = VAULT_PATH_KV1_1 + "," + VAULT_PATH_KV1_2),
        @Env(name = "CASC_VAULT_ENGINE_VERSION", value = "1")
    })
    public void kv2WithUserDeprecatedPath() {
        assertThat(SecretSourceResolver.resolve(context, "${key3}"), equalTo("789"));
    }
}
