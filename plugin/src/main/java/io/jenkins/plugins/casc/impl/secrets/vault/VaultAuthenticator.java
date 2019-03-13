package io.jenkins.plugins.casc.impl.secrets.vault;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;

public interface VaultAuthenticator {
    void authenticate(Vault vault, VaultConfig config) throws VaultException;
}
