package io.jenkins.plugins.casc.impl.secrets.util;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;

import java.util.logging.Logger;

public class VaultSingleTokenAuthenticator implements VaultAuthenticator {
    private final static Logger LOGGER = Logger.getLogger(VaultSingleTokenAuthenticator.class.getName());

    private String token;

    public VaultSingleTokenAuthenticator(String token) {
        this.token = token;
    }

    public void authenticate(Vault vault, VaultConfig config) throws VaultException {
        // No special mechanism - token already exists
        config.token(token);
        config.build();
        return;
    }
}
