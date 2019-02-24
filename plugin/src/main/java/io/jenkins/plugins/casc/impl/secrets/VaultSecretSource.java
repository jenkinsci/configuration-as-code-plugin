package io.jenkins.plugins.casc.impl.secrets;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import hudson.Extension;
import io.jenkins.plugins.casc.SecretSource;
import org.apache.commons.lang.StringUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Requires either CASC_VAULT_USER and CASC_VAULT_PW, or CASC_VAULT_TOKEN environment variables set
 * alongside with CASC_VAULT_PATH and CASC_VAULT_URL
 */
@Extension

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
        String vaultToken = getVariable("CASC_VAULT_TOKEN", prop);
        String vaultAppRole = getVariable("CASC_VAULT_APPROLE", prop);
        String vaultAppRoleSecret = getVariable("CASC_VAULT_APPROLE_SECRET", prop);
        String vaultNamespace = getVariable("CASC_VAULT_NAMESPACE", prop);
        String vaultEngineVersion = getVariable("CASC_VAULT_ENGINE_VERSION", prop);

        if(((vaultPw != null && vaultUsr != null) || 
            vaultToken != null || 
            (vaultAppRole != null && vaultAppRoleSecret != null)) && vaultPth != null && vaultUrl != null) {
            LOGGER.log(Level.FINE, "Attempting to connect to Vault: {0}", vaultUrl);
            try {
                VaultConfig config = new VaultConfig().address(vaultUrl);
                if (vaultNamespace != null) {
                    // optionally set namespace
                    config = config.nameSpace(vaultNamespace);
                    LOGGER.log(Level.FINE, "Using namespace with Vault: {0}", vaultNamespace);
                }
                if (vaultEngineVersion != null) {
                    // optionally set vault engine version
                    config = config.engineVersion( Integer.parseInt(vaultEngineVersion) );
                    LOGGER.log(Level.FINE, "Using engine version: {0}", vaultEngineVersion);
                }
                config = config.build();
                Vault vault = new Vault(config);
                //Obtain a login token
                final String token;
                if (vaultToken != null) {
                    token = vaultToken;
                    LOGGER.log(Level.FINE, "Using supplied token to access Vault");
                } else if (vaultAppRole != null && vaultAppRoleSecret != null) {
                    token = vault.auth().loginByAppRole(vaultAppRole, vaultAppRoleSecret).getAuthClientToken();
                    LOGGER.log(Level.FINE, "Login to Vault using AppRole/SecretID successful");
                } else {
                    token = vault.auth().loginByUserPass(vaultUsr, vaultPw, vaultMount).getAuthClientToken();
                    LOGGER.log(Level.FINE, "Login to Vault using U/P successful");
                }
                config.token(token).build();
                secrets = vault.logical().read(vaultPth).getData();
            } catch (VaultException ve) {
                LOGGER.log(Level.WARNING, "Unable to connect to Vault", ve);
            }
        }
    }

    @Override
    public Optional<String> reveal(String secret) {
        if (StringUtils.isBlank(secret)) return Optional.empty();
        return Optional.ofNullable(secrets.get(secret));
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
