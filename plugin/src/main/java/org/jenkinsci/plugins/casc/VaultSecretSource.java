package org.jenkinsci.plugins.casc;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import hudson.Extension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Replaces secrets from .yaml files with the ${vault.*} prefix
 *
 * For now requires 4 environment variables set.
 */
@Extension
public class VaultSecretSource extends SecretSource {

    private final static Logger LOGGER = Logger.getLogger(VaultSecretSource.class.getName());
    private Map<String, String> secrets = new HashMap<>();

    public VaultSecretSource() {
        String vaultPw = System.getenv("CASC_VAULT_PW");
        String vaultUsr = System.getenv("CASC_VAULT_USER");
        String vaultPth = System.getenv("CASC_VAULT_PATH");
        String vaultUrl = System.getenv("CASC_VAULT_URL");
        String vaultMount = System.getenv("CASC_VAULT_MOUNT");
        if(vaultPw != null && vaultUsr != null && vaultPth != null && vaultUrl != null) {
            LOGGER.log(Level.FINE,"Attempting to connect to Vault: {0}", vaultUrl);
            try {
                VaultConfig config = new VaultConfig().address(vaultUrl).build();
                Vault vault = new Vault(config);
                //Obtain a login token
                final String token = vault.auth().loginByUserPass(vaultUsr, vaultPw, vaultMount).getAuthClientToken();
                LOGGER.log(Level.FINE, "Login to Vault successful");
                config.token(token).build();
                secrets = vault.logical().read(vaultPth).getData();
            } catch (VaultException ve) {
                LOGGER.log(Level.WARNING, "Unable to fetch password from vault", ve);
            }
        }
    }

    @Override
    public Optional<String> reveal(String vaultKey) {
        Optional<String> returnValue = Optional.empty();
        if(secrets.containsKey(vaultKey))  {
            returnValue = Optional.of(secrets.get(vaultKey));
        }
        return returnValue;
    }

    public Map<String, String> getSecrets() {
        return secrets;
    }

    public void setSecrets(Map<String, String> secrets) {
        this.secrets = secrets;
    }
}
