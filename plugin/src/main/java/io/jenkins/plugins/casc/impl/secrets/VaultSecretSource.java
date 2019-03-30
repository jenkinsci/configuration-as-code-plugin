package io.jenkins.plugins.casc.impl.secrets;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import hudson.Extension;
import io.jenkins.plugins.casc.SecretSource;
import io.jenkins.plugins.casc.impl.secrets.vault.VaultAppRoleAuthenticator;
import io.jenkins.plugins.casc.impl.secrets.vault.VaultAuthenticator;
import io.jenkins.plugins.casc.impl.secrets.vault.VaultSingleTokenAuthenticator;
import io.jenkins.plugins.casc.impl.secrets.vault.VaultUserPassAuthenticator;
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
 * Requires either CASC_VAULT_USER and CASC_VAULT_PW, or CASC_VAULT_TOKEN,
 * or CASC_VAULT_APPROLE and CASC_VAULT_APPROLE_SECRET
 * environment variables set alongside with CASC_VAULT_PATHS and CASC_VAULT_URL
 */
@Extension

public class VaultSecretSource extends SecretSource {

    private final static Logger LOGGER = Logger.getLogger(VaultSecretSource.class.getName());

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

    private Map<String, String> secrets = new HashMap<>();
    private Vault vault;
    private VaultConfig vaultConfig;
    private VaultAuthenticator vaultAuthenticator;
    private String[] vaultPaths;

    public VaultSecretSource() {
        Optional<String> vaultFile = Optional.ofNullable(System.getenv(CASC_VAULT_FILE));
        Properties prop = new Properties();
        vaultFile.ifPresent(file -> readPropertiesFromVaultFile(file, prop));

        // Parse variables
        Optional<String> vaultEngineVersionOpt = getVariable(CASC_VAULT_ENGINE_VERSION, prop);
        Optional<String> vaultUrl = getVariable(CASC_VAULT_URL, prop);
        Optional<String> vaultNamespace = getVariable(CASC_VAULT_NAMESPACE, prop);
        Optional<String[]> vaultPaths = getCommaSeparatedVariables(CASC_VAULT_PATHS, prop)
                .map(Optional::of)
                .orElseGet(() -> getCommaSeparatedVariables(CASC_VAULT_PATH, prop)); // TODO: deprecate!

        // Check mandatory variables are set
        if (!vaultUrl.isPresent() || !vaultPaths.isPresent()) return;

        String vaultEngineVersion = vaultEngineVersionOpt.orElse(DEFAULT_ENGINE_VERSION);
        this.vaultPaths = vaultPaths.get();
        determineAuthenticator(prop);

        // configure vault client
        vaultConfig = new VaultConfig().address(vaultUrl.get());
        try {
            LOGGER.log(Level.FINE, "Attempting to connect to Vault: {0}", vaultUrl);
            if (vaultNamespace.isPresent()) {
                vaultConfig.nameSpace(vaultNamespace.get());
                LOGGER.log(Level.FINE, "Using namespace with Vault: {0}", vaultNamespace);
            }

            vaultConfig.engineVersion(Integer.parseInt(vaultEngineVersion));
            LOGGER.log(Level.FINE, "Using engine version: {0}", vaultEngineVersion);
        } catch (VaultException e) {
            LOGGER.log(Level.WARNING, "Could not configure vault connection", e);
        }

        try {
            vaultConfig.build();
        } catch (VaultException e) {
            LOGGER.log(Level.WARNING, "Could not configure vault client", e);
        }

        vault = new Vault(vaultConfig);
    }

    private void determineAuthenticator(Properties prop) {
        Optional<String> vaultPw = getVariable(CASC_VAULT_PW, prop);
        Optional<String> vaultUser = getVariable(CASC_VAULT_USER, prop);
        Optional<String> vaultMount = getVariable(CASC_VAULT_MOUNT, prop);
        Optional<String> vaultToken = getVariable(CASC_VAULT_TOKEN, prop);
        Optional<String> vaultAppRole = getVariable(CASC_VAULT_APPROLE, prop);
        Optional<String> vaultAppRoleSecret = getVariable(CASC_VAULT_APPROLE_SECRET, prop);

        vaultToken.ifPresent(
                token -> vaultAuthenticator = new VaultSingleTokenAuthenticator(token)
        );

        vaultUser.ifPresent(
                user -> vaultPw.ifPresent(
                        pw -> vaultAuthenticator = new VaultUserPassAuthenticator(user, pw, vaultMount.orElse(DEFAULT_USER_BACKEND))
                )
        );

        vaultAppRole.ifPresent(
                approle -> vaultAppRoleSecret.ifPresent(
                        approleSecret -> vaultAuthenticator = new VaultAppRoleAuthenticator(approle, approleSecret)
                )
        );

        if (vaultAuthenticator == null) {
            LOGGER.log(Level.WARNING, "Could not determine vault authentication method. Not able to read secrets from vault.");
        }
    }

    private void readSecretsFromVault() {
        Optional<String[]> vaultPathsOpt = Optional.ofNullable(vaultPaths);

        if (vaultPathsOpt.isPresent()) {
            try {
                // refresh map
                secrets = new HashMap<>();

                // Parse secrets
                for (String vaultPath : vaultPathsOpt.get()) {

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
            } catch (VaultException e) {
                LOGGER.log(Level.WARNING, "Unable to fetch secret from Vault", e);
            }
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
        Optional<String[]> strings = getVariable(key, prop).map(str -> str.split(","));
        if (key.equals(CASC_VAULT_PATH) && strings.isPresent())
             LOGGER.log(Level.WARNING, "[Deprecation Warning] CASC_VAULT_PATH is deprecated. " +
                    "Please use CASC_VAULT_PATHS instead."); // TODO: deprecate!
        return strings;
    }

    @Override
    public void init() {
        // Ensure secrets are up-to-date and Check vault authentication
        if (vaultAuthenticator != null) {
            try {
                vaultAuthenticator.authenticate(vault, vaultConfig);
            } catch (VaultException e) {
                LOGGER.log(Level.WARNING, "Could not authenticate with vault client", e);
            }
            readSecretsFromVault();
        }
    }
}
