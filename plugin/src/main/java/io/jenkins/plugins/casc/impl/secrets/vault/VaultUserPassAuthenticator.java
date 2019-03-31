package io.jenkins.plugins.casc.impl.secrets.vault;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.codec.digest.DigestUtils;

public class VaultUserPassAuthenticator extends VaultAuthenticatorWithExpiration {
    private final static Logger LOGGER = Logger.getLogger(VaultUserPassAuthenticator.class.getName());

    private String user;
    private String pass;
    private String mountPath;
    private String currentAuthToken;

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

    public String getAttributeHash() {
        return DigestUtils.sha256Hex(
                DigestUtils.sha256Hex(user)
                        + pass
                        + mountPath
        );
    }
}
