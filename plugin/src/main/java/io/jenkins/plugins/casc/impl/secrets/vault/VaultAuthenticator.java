package io.jenkins.plugins.casc.impl.secrets.vault;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;

public interface VaultAuthenticator {
    void authenticate(Vault vault, VaultConfig config) throws VaultException;
    static VaultAuthenticator of(String token) {
        return new VaultSingleTokenAuthenticator(token);
    }
    static VaultAuthenticator of(String approle, String approleSecret) {
        return new VaultAppRoleAuthenticator(approle, approleSecret);
    }
    static VaultAuthenticator of(String user, String pass, String mouthPath) {
        return new VaultUserPassAuthenticator(user, pass, mouthPath);
    }
}
