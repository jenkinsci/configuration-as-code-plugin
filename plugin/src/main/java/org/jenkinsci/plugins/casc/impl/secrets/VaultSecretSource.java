package org.jenkinsci.plugins.casc.impl.secrets;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import hudson.Extension;
import org.jenkinsci.plugins.casc.SecretSource;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.Beta;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Replaces secrets from .yaml files with the ${vault.*} prefix
 *
 * For now requires 4 environment variables set.
 */
@Extension
@Restricted(Beta.class)
public class VaultSecretSource extends SecretSource {

    private final static Logger LOGGER = Logger.getLogger(VaultSecretSource.class.getName());
    private Map<String, String> secrets = new HashMap<>();

    public VaultSecretSource() {
        String vaultFile = System.getenv("CASC_VAULT_FILE");
        Properties prop = new Properties();
        if (vaultFile != null) {
            try (FileInputStream input = new FileInputStream(vaultFile)) {
                prop.load(input);
                if (prop.isEmpty()) {
                    LOGGER.log(Level.WARNING, "Vault secret file is empty");
                }
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, "Failed to load Vault secrets from file", ex);
            }
        }
        String vaultPw = getVariable("CASC_VAULT_PW", prop);
        String vaultUsr = getVariable("CASC_VAULT_USER", prop);
        String vaultPth = getVariable("CASC_VAULT_PATH", prop);
        String vaultUrl = getVariable("CASC_VAULT_URL", prop);
        String vaultMount = getVariable("CASC_VAULT_MOUNT", prop);

        if(vaultPw != null && vaultUsr != null && vaultPth != null && vaultUrl != null) {
            LOGGER.log(Level.FINE, "Attempting to connect to Vault: {0}", vaultUrl);
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

    private String getVariable(String key, Properties prop) {
        if (prop != null && !prop.isEmpty()) {
            return prop.getProperty(key, System.getenv(key));
        } else {
            return System.getenv(key);
        }
    }
}
