package org.jenkinsci.plugins.casc;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import hudson.Extension;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Replaces secrets from .yaml files with the ${vault.*} prefix
 *
 * For now requires 4 environment variables set.
 */
@Extension
public class VaultSecretSource extends SecretSource {

    private final static Logger LOGGER = Logger.getLogger(VaultSecretSource.class.getName());

    @Override
    public String reveal(String fromKey) {
        String vaultPw = System.getenv("CASC_VAULT_PW");
        String vaultUsr = System.getenv("CASC_VAULT_USER");
        String vaultPth = System.getenv("CASC_VAULT_PATH");
        String vaultUrl = System.getenv("CASC_VAULT_URL");
        if(vaultPw != null && vaultUsr != null && vaultPth != null && vaultUrl != null) {
            try {
                Matcher m = SecretSource.SECRET_PATTERN.matcher(fromKey);
                if (m.matches()) {
                    final String vaultKey = m.group(1);
                    VaultConfig config = new VaultConfig().address(vaultUrl).build();
                    Vault vault = new Vault(config);
                    //Obtain a login token
                    final String token = vault.auth().loginByUserPass(vaultUsr, vaultPw).getAuthClientToken();
                    config.token(token).build();
                    //Fetch
                    String value = vault.logical().read(vaultPth).getData().get(vaultKey);
                    if (value == null)
                        throw new IllegalArgumentException(String.format("Secret could not be fetched from vault with key: %s from path: %s", vaultKey, vaultPth));
                    return value;
                }
            } catch (VaultException ve) {
                LOGGER.log(Level.WARNING, "Unable to fetch password from vault", ve);
                return null;
            }
        }
        return null;
    }

}
