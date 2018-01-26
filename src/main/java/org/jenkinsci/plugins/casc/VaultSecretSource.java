package org.jenkinsci.plugins.casc;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import hudson.Extension;

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

    @Override
    public Optional<String> reveal(String vaultKey) {
        String vaultPw = System.getenv("CASC_VAULT_PW");
        String vaultUsr = System.getenv("CASC_VAULT_USER");
        String vaultPth = System.getenv("CASC_VAULT_PATH");
        String vaultUrl = System.getenv("CASC_VAULT_URL");
        Optional<String> returnValue = Optional.empty();
        if(vaultPw != null && vaultUsr != null && vaultPth != null && vaultUrl != null) {
            try {
                VaultConfig config = new VaultConfig().address(vaultUrl).build();
                Vault vault = new Vault(config);
                //Obtain a login token
                final String token = vault.auth().loginByUserPass(vaultUsr, vaultPw).getAuthClientToken();
                config.token(token).build();
                //Fetch
                String v = vault.logical().read(vaultPth).getData().get(vaultKey);
                if (v != null)
                    returnValue = Optional.of(v);
                return returnValue;
            } catch (VaultException ve) {
                LOGGER.log(Level.WARNING, "Unable to fetch password from vault", ve);
                return returnValue;
            }
        }
        return returnValue;
    }

}
