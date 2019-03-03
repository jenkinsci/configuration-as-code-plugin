package io.jenkins.plugins.casc.vault;


import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.EnvVarsRule;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import jenkins.model.Jenkins;
import org.junit.*;
import org.junit.rules.RuleChain;
import org.testcontainers.vault.VaultContainer;

import static org.junit.Assert.assertEquals;

// Inspired by https://github.com/BetterCloud/vault-java-driver/blob/master/src/test-integration/java/com/bettercloud/vault/util/VaultContainer.java
public class KV1WithTokenTest {

    @ClassRule
    public static VaultContainer vaultContainer = VaultTestUtil.createVaultContainer();

    @BeforeClass
    public static void configureVaultContainer() {
        // Dont run on non-docker daemon nodes
        org.junit.Assume.assumeTrue(VaultTestUtil.hasDockerDaemon());
        VaultTestUtil.configureVaultContainer(vaultContainer);
    }

    @Rule
    public RuleChain chain = RuleChain.outerRule(new EnvVarsRule()
            .env("CASC_VAULT_URL", VaultTestUtil.VAULT_URL)
            .env("CASC_VAULT_TOKEN", VaultTestUtil.VAULT_ROOT_TOKEN)
            .env("CASC_VAULT_PATH", VaultTestUtil.VAULT_PATH_V1)
            .env("CASC_VAULT_ENGINE_VERSION", "1"))
            .around(new JenkinsConfiguredWithCodeRule());

    @Test
    @ConfiguredWithCode("vaultTest_jenkins.yml")
    public void kv1_with_token() {
        Jenkins j = Jenkins.getInstance();
        assertEquals("key1: 123", j.getSystemMessage());
    }
}
