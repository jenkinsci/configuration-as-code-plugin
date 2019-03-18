package io.jenkins.plugins.casc.impl.secrets.vault;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;

public class VaultSingleTokenAuthenticator implements VaultAuthenticator {
    private String token;

    public VaultSingleTokenAuthenticator(String token) {
        this.token = token;
    }

    public void authenticate(Vault vault, VaultConfig config) throws VaultException {
        // No special mechanism - token already exists
        config.token(token).build();
    }
}
