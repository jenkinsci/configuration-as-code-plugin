package io.jenkins.plugins.casc.impl.secrets;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import hudson.Extension;
import io.jenkins.plugins.casc.SecretSource;
import org.apache.commons.lang.StringUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Requires either CASC_VAULT_USER and CASC_VAULT_PW, or CASC_VAULT_TOKEN environment variables set
 * alongside with CASC_VAULT_PATHS and CASC_VAULT_URL
 */
@Extension

public class VaultSecretSource extends SecretSource {

    private final static Logger LOGGER = Logger.getLogger(VaultSecretSource.class.getName());

    // Constants
    private static final String CASC_VAULT_FILE = "CASC_VAULT_FILE";
    private static final String CASC_VAULT_PW = "CASC_VAULT_PW";
    private static final String CASC_VAULT_USER = "CASC_VAULT_USER";
    private static final String CASC_VAULT_URL = "CASC_VAULT_URL";
    private static final String CASC_VAULT_MOUNT = "CASC_VAULT_MOUNT";
    private static final String CASC_VAULT_TOKEN = "CASC_VAULT_TOKEN";
    private static final String CASC_VAULT_APPROLE = "CASC_VAULT_APPROLE";
    private static final String CASC_VAULT_APPROLE_SECRET = "CASC_VAULT_APPROLE_SECRET";
    private static final String CASC_VAULT_NAMESPACE = "CASC_VAULT_NAMESPACE";
    private static final String CASC_VAULT_ENGINE_VERSION = "CASC_VAULT_ENGINE_VERSION";
    private static final String CASC_VAULT_PATHS = "CASC_VAULT_PATHS";
    private static final String CASC_VAULT_PATH = "CASC_VAULT_PATH"; // TODO: deprecate!
    private static final String DEFAULT_ENGINE_VERSION = "2";
    private static final String DEFAULT_USER_BACKEND = "userpass";

    // Vars
    private Map<String, String> secrets = new HashMap<>();
    private Vault vault;
    private VaultConfig vaultConfig;
    private Optional<String[]> vaultPaths;
    private Optional<String> vaultPw;
    private Optional<String> vaultUser;
    private Optional<String> vaultUrl;
    private Optional<String> vaultMount;
    private Optional<String> vaultToken;
    private Optional<String> vaultAppRole;
    private Optional<String> vaultAppRoleSecret;
    private Optional<String> vaultNamespace;
    private Optional<String> vaultEngineVersion;


    public VaultSecretSource() {
        Optional<String> vaultFile = Optional.ofNullable(System.getenv(CASC_VAULT_FILE));
        Properties prop = new Properties();
        vaultFile.ifPresent(file -> readPropertiesFromVaultFile(file, prop));

        // Parse variables
        vaultPw = getVariable(CASC_VAULT_PW, prop);
        vaultUser = getVariable(CASC_VAULT_USER, prop);
        vaultUrl = getVariable(CASC_VAULT_URL, prop);
        vaultMount = getVariable(CASC_VAULT_MOUNT, prop);
        vaultToken = getVariable(CASC_VAULT_TOKEN, prop);
        vaultAppRole = getVariable(CASC_VAULT_APPROLE, prop);
        vaultAppRoleSecret = getVariable(CASC_VAULT_APPROLE_SECRET, prop);
        vaultNamespace = getVariable(CASC_VAULT_NAMESPACE, prop);
        vaultEngineVersion = getVariable(CASC_VAULT_ENGINE_VERSION, prop);
        vaultPaths = getCommaSeparatedVariables(CASC_VAULT_PATHS, prop)
                .map(Optional::of)
                .orElse(getCommaSeparatedVariables(CASC_VAULT_PATH, prop)); // TODO: deprecate!

        // Check mandatory variables are set
        if (!vaultUrl.isPresent() || !vaultPaths.isPresent()) return;

        // Check defaults
        if (!vaultMount.isPresent()) vaultMount = Optional.of(DEFAULT_USER_BACKEND);
        if (!vaultEngineVersion.isPresent()) vaultEngineVersion = Optional.of(DEFAULT_ENGINE_VERSION);

        // configure vault client
        vaultConfig = new VaultConfig().address(vaultUrl.get());
        try {
            LOGGER.log(Level.FINE, "Attempting to connect to Vault: {0}", vaultUrl.get());
            if (vaultNamespace.isPresent()) {
                vaultConfig.nameSpace(vaultNamespace.get());
                LOGGER.log(Level.FINE, "Using namespace with Vault: {0}", vaultNamespace);
            }

            vaultConfig.engineVersion(Integer.parseInt(vaultEngineVersion.get()));
            LOGGER.log(Level.FINE, "Using engine version: {0}", vaultEngineVersion);

            vaultConfig = vaultConfig.build();
        } catch (VaultException e) {
            LOGGER.log(Level.WARNING, "Could not configure vault connection", e);
        }

        try {
            vaultConfig.build();
        } catch (VaultException e) {
            LOGGER.log(Level.WARNING, "Could not configure vault client", e);
        }
        vault = new Vault(vaultConfig);

        authenticate();

        readSecretsFromVault();
    }

    private void authenticate() {
        Optional<String> authToken = Optional.empty();

        // attempt token login
        if (vaultToken.isPresent()) {
            authToken = vaultToken;
            LOGGER.log(Level.FINE, "Using supplied token to access Vault");
        }

        // attempt AppRole login
        if (vaultAppRole.isPresent() && vaultAppRoleSecret.isPresent() && !authToken.isPresent()) {
            try {
                authToken = Optional.ofNullable(
                        vault.auth().loginByAppRole(vaultAppRole.get(), vaultAppRoleSecret.get()).getAuthClientToken()
                );
                LOGGER.log(Level.FINE, "Login to Vault using AppRole/SecretID successful");
            } catch (VaultException e) {
                LOGGER.log(Level.WARNING, "Could not login with AppRole", e);
            }
        }

        // attempt User/Pass login
        if (vaultUser.isPresent() && vaultPw.isPresent() && !authToken.isPresent()) {
            try {
                authToken = Optional.ofNullable(
                        vault.auth().loginByUserPass(vaultUser.get(), vaultPw.get(), vaultMount.get()).getAuthClientToken()
                );
                LOGGER.log(Level.FINE, "Login to Vault using User/Pass successful");
            } catch (VaultException e) {
                LOGGER.log(Level.WARNING, "Could not login with User/Pass", e);
            }
        }

        // Set auth token
        try {
            if (authToken.isPresent()) {
                vaultConfig.token(authToken.get()).build();
            } else {
                LOGGER.log(Level.WARNING, "authToken not present");
            }
        } catch (VaultException e) {
            LOGGER.log(Level.WARNING, "Could not set auth token in vault client", e);
        }
    }

    private void readSecretsFromVault() {
        try {
            if (vaultPaths.isPresent()) {
                for (String vaultPath : vaultPaths.get()) {

                    // check if we overwrite an existing key from another path
                    Map<String, String> nextSecrets = vault.logical().read(vaultPath).getData();
                    for (String key : nextSecrets.keySet()) {
                        if (secrets.containsKey(key)) {
                            LOGGER.log(Level.WARNING, "Key {0} exists in multiple vault paths.", key);
                        }
                    }

                    // merge
                    secrets.putAll(nextSecrets);
                }
            } else {
                LOGGER.log(Level.WARNING, "Vault paths not set");
            }
        } catch (VaultException e) {
            LOGGER.log(Level.WARNING, "Unable to fetch secret from Vault", e);
        }
    }

    private void readPropertiesFromVaultFile(String vaultFile, Properties prop) {
        try (FileInputStream input = new FileInputStream(vaultFile)) {
            prop.load(input);
            if (prop.isEmpty()) {
                LOGGER.log(Level.WARNING, "Vault secret file is empty");
            }
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Failed to load Vault secrets from file", ex);
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

    private Optional<String> getVariable(String key, Properties prop) {
        return Optional.ofNullable(prop.getProperty(key, System.getenv(key)));
    }

    private Optional<String[]> getCommaSeparatedVariables(String key, Properties prop) {
        if (key.equals(CASC_VAULT_PATH)) LOGGER.log(Level.WARNING, "[Deprecation Warning] CASC_VAULT_PATH will be deprecated. " +
                "Please use CASC_VAULT_PATHS instead."); // TODO: deprecate!
        return getVariable(key, prop).map(str -> str.split(","));
    }
}
