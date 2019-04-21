package io.jenkins.plugins.casc.impl.secrets.vault;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VaultUserPassAuthenticator extends VaultAuthenticatorWithExpiration {
    private final static Logger LOGGER = Logger.getLogger(VaultUserPassAuthenticator.class.getName());

    private String user;
    private String pass;
    private String mountPath;

    public VaultUserPassAuthenticator(String user, String pass, String mountPath) {
        this.user = user;
        this.pass = pass;
        this.mountPath = mountPath;
    }

    public void authenticate(Vault vault, VaultConfig config) throws VaultException {
        if (isTokenTTLExpired()) {
            // authenticate
            currentAuthToken = vault.auth().loginByUserPass(user, pass, mountPath).getAuthClientToken();
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
        return Objects.hash(user, pass, mountPath);
    }
}
