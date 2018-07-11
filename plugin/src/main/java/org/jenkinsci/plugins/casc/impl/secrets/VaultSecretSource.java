/*
 * Copyright (c) 2018 CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to
 * whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkinsci.plugins.casc.impl.secrets;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import hudson.Extension;
import org.jenkinsci.plugins.casc.SecretSource;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.Beta;

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
@Restricted(Beta.class)
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
            try {
                VaultConfig config = new VaultConfig().address(vaultUrl).build();
                Vault vault = new Vault(config);
                //Obtain a login token
                final String token = vault.auth().loginByUserPass(vaultUsr, vaultPw, vaultMount).getAuthClientToken();
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
