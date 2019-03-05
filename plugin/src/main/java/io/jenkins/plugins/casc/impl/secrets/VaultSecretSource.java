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
 * Requires either CASC_VAULT_USER and CASC_VAULT_PW, or CASC_VAULT_TOKEN,
 * or CASC_VAULT_APPROLE and CASC_VAULT_APPROLE_SECRET
 * environment variables set alongside with CASC_VAULT_PATHS and CASC_VAULT_URL
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

    // Var fields
    private Map<String, String> secrets = new HashMap<>();
    private Vault vault;
    private VaultConfig vaultConfig;
    private String[] vaultPaths;
    private String vaultPw;
    private String vaultUser;
    private String vaultMount;
    private String vaultToken;
    private String vaultAppRole;
    private String vaultAppRoleSecret;
    private Calendar authTokenExpiration;


    public VaultSecretSource() {
        authTokenExpiration = Calendar.getInstance();
        Optional<String> vaultFile = Optional.ofNullable(System.getenv(CASC_VAULT_FILE));
        Properties prop = new Properties();
        vaultFile.ifPresent(file -> readPropertiesFromVaultFile(file, prop));

        // Parse variables
        Optional<String> vaultEngineVersionOpt = getVariable(CASC_VAULT_ENGINE_VERSION, prop);
        Optional<String> vaultUrlOpt = getVariable(CASC_VAULT_URL, prop);
        Optional<String> vaultNamespaceOpt = getVariable(CASC_VAULT_NAMESPACE, prop);
        Optional<String> vaultPwOpt = getVariable(CASC_VAULT_PW, prop);
        Optional<String> vaultUserOpt = getVariable(CASC_VAULT_USER, prop);
        Optional<String> vaultMountOpt = getVariable(CASC_VAULT_MOUNT, prop);
        Optional<String> vaultTokenOpt = getVariable(CASC_VAULT_TOKEN, prop);
        Optional<String> vaultAppRoleOpt = getVariable(CASC_VAULT_APPROLE, prop);
        Optional<String> vaultAppRoleSecretOpt = getVariable(CASC_VAULT_APPROLE_SECRET, prop);
        Optional<String[]> vaultPathsOpt = getCommaSeparatedVariables(CASC_VAULT_PATHS, prop)
                .map(Optional::of)
                .orElse(getCommaSeparatedVariables(CASC_VAULT_PATH, prop)); // TODO: deprecate!

        // Check mandatory variables are set
        if (!vaultUrlOpt.isPresent() || !vaultPathsOpt.isPresent()) return;

        // Check defaults
        vaultMount = vaultMountOpt.orElse(DEFAULT_USER_BACKEND);
        String vaultEngineVersion = vaultEngineVersionOpt.orElse(DEFAULT_ENGINE_VERSION);

        // Set class fields
        String vaultUrl = vaultUrlOpt.get();
        vaultPw = vaultPwOpt.orElse(null);
        vaultUser = vaultUserOpt.orElse(null);
        vaultMount = vaultMountOpt.orElse(null);
        vaultToken = vaultTokenOpt.orElse(null);
        vaultAppRole = vaultAppRoleOpt.orElse(null);
        vaultAppRoleSecret = vaultAppRoleSecretOpt.orElse(null);
        vaultPaths = vaultPathsOpt.orElse(null);

        // configure vault client
        vaultConfig = new VaultConfig().address(vaultUrl);
        try {
            LOGGER.log(Level.FINE, "Attempting to connect to Vault: {0}", vaultUrl);
            if (vaultNamespaceOpt.isPresent()) {
                vaultConfig.nameSpace(vaultNamespaceOpt.get());
                LOGGER.log(Level.FINE, "Using namespace with Vault: {0}", vaultNamespaceOpt);
            }

            vaultConfig.engineVersion(Integer.parseInt(vaultEngineVersion));
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
    }

    private boolean needNewToken() {
        boolean result = true;
        Calendar now = Calendar.getInstance();
        long timeDiffInMillis = now.getTimeInMillis() - authTokenExpiration.getTimeInMillis();
        if (timeDiffInMillis < -5000L) {
            // token will be valid for at least another 5s
            result = false;
            LOGGER.log(Level.FINE, "Auth token is still valid");
        } else {
            LOGGER.log(Level.FINE, "Auth token has to be re-issued" + timeDiffInMillis);
        }

        return result;
    }

    private void setAuthTokenInVaultClient(String authToken) {
        // Set auth token
        try {
            vaultConfig.token(authToken).build();
        } catch (VaultException e) {
            LOGGER.log(Level.WARNING, "Could not set auth token in vault client", e);
        }

        // Set token expiration date
        try {
            authTokenExpiration = Calendar.getInstance();
            authTokenExpiration.add(Calendar.SECOND, (int) vault.auth().lookupSelf().getTTL());
        } catch (VaultException e) {
            LOGGER.log(Level.WARNING, "Could not determine token expiration. Assuming expired soon.", e);
            authTokenExpiration = Calendar.getInstance();
        }
    }

    private void authenticate() {
        Optional<String> vaultTokenOpt = Optional.ofNullable(vaultToken);
        Optional<String> vaultAppRoleOpt = Optional.ofNullable(vaultAppRole);
        Optional<String> vaultAppRoleSecretOpt = Optional.ofNullable(vaultAppRoleSecret);
        Optional<String> vaultUserOpt = Optional.ofNullable(vaultUser);
        Optional<String> vaultPwOpt = Optional.ofNullable(vaultPw);

        if (!(vaultTokenOpt.isPresent() ||
                (vaultAppRoleOpt.isPresent() && vaultAppRoleSecretOpt.isPresent()) ||
                (vaultUserOpt.isPresent() && vaultPwOpt.isPresent()))
        ) {
            // Options are not configured for vault access
            return;
        }

        if (needNewToken()) {
            Optional<String> authTokenOpt = Optional.empty();

            // attempt token login
            if (vaultTokenOpt.isPresent()) {
                authTokenOpt = vaultTokenOpt;
                LOGGER.log(Level.FINE, "Using supplied token to access Vault");
            }

            // attempt AppRole login
            if (vaultAppRoleOpt.isPresent() && vaultAppRoleSecretOpt.isPresent() && !authTokenOpt.isPresent()) {
                try {
                    authTokenOpt = Optional.ofNullable(
                            vault.auth().loginByAppRole(vaultAppRoleOpt.get(), vaultAppRoleSecretOpt.get()).getAuthClientToken()
                    );
                    LOGGER.log(Level.FINE, "Login to Vault using AppRole/SecretID successful");
                } catch (VaultException e) {
                    LOGGER.log(Level.WARNING, "Could not login with AppRole", e);
                }
            }

            // attempt User/Pass login
            if (vaultUserOpt.isPresent() && vaultPwOpt.isPresent() && !authTokenOpt.isPresent()) {
                try {
                    authTokenOpt = Optional.ofNullable(
                            vault.auth().loginByUserPass(vaultUserOpt.get(), vaultPwOpt.get(), vaultMount).getAuthClientToken()
                    );
                    LOGGER.log(Level.FINE, "Login to Vault using User/Pass successful");
                } catch (VaultException e) {
                    LOGGER.log(Level.WARNING, "Could not login with User/Pass", e);
                }
            }

            if (authTokenOpt.isPresent()) {
                setAuthTokenInVaultClient(authTokenOpt.get());
            } else {
                LOGGER.log(Level.WARNING, "Vault auth token is null.");
            }
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

        // TODO: move this to SecretSource.init() function which gets called only once when CasC.configure() is run
        // Ensure secrets are up-to-date
        authenticate();
        readSecretsFromVault();

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
        if (key.equals(CASC_VAULT_PATH))
            LOGGER.log(Level.WARNING, "[Deprecation Warning] CASC_VAULT_PATH will be deprecated. " +
                    "Please use CASC_VAULT_PATHS instead."); // TODO: deprecate!
        return getVariable(key, prop).map(str -> str.split(","));
    }
}
