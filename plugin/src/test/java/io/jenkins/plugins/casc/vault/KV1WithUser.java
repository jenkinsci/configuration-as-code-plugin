package io.jenkins.plugins.casc.vault;


import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.EnvVarsRule;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import jenkins.model.Jenkins;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.testcontainers.vault.VaultContainer;

import static org.junit.Assert.assertEquals;

// Inspired by https://github.com/BetterCloud/vault-java-driver/blob/master/src/test-integration/java/com/bettercloud/vault/util/VaultContainer.java
public class KV1WithUser {

    @ClassRule
    public static VaultContainer vaultContainer = VaultTestUtil.createVaultContainer();

    @BeforeClass
    public static void configureVaultContainer() {
        VaultTestUtil.configureVaultContainer(vaultContainer);
    }

    @Rule
    public RuleChain chain = RuleChain.outerRule(new EnvVarsRule()
            .env("CASC_VAULT_URL", VaultTestUtil.VAULT_URL)
            .env("CASC_VAULT_USER", VaultTestUtil.VAULT_USER)
            .env("CASC_VAULT_PW", VaultTestUtil.VAULT_PW)
            .env("CASC_VAULT_PATH", VaultTestUtil.VAULT_PATH_V1)
            .env("CASC_VAULT_ENGINE_VERSION", "1"))
            .around(new JenkinsConfiguredWithCodeRule());

    @Test
    @ConfiguredWithCode("vaultTest_jenkins.yml")
    public void kv1_with_user() {
        Jenkins j = Jenkins.getInstance();
        assertEquals("key1: 123", j.getSystemMessage());
    }
}
