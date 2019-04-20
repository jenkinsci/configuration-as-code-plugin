package io.jenkins.plugins.casc.impl.secrets.vault;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VaultAppRoleAuthenticator extends VaultAuthenticatorWithExpiration {
    private final static Logger LOGGER = Logger.getLogger(VaultAppRoleAuthenticator.class.getName());

    private String approle;
    private String approleSecret;

    public VaultAppRoleAuthenticator(String approle, String approleSecret) {
        this.approle = approle;
        this.approleSecret = approleSecret;
    }

    public void authenticate(Vault vault, VaultConfig config) throws VaultException {
        if (isTokenTTLExpired()) {
            // authenticate
            currentAuthToken = vault.auth().loginByAppRole(approle, approleSecret).getAuthClientToken();
            config.token(currentAuthToken).build();
            LOGGER.log(Level.FINE, "Login to Vault using AppRole/SecretID successful");
            getTTLExpiryOfCurrentToken(vault);
        } else {
            // make sure current auth token is set in config
            config.token(currentAuthToken).build();
        }
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(approle, approleSecret);
    }
}
