package io.jenkins.plugins.casc.impl.secrets.vault;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultException;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

abstract class VaultAuthenticatorWithExpiration implements VaultAuthenticator {

    private final static Logger LOGGER = Logger.getLogger(VaultAuthenticatorWithExpiration.class.getName());

    private Calendar tokenExpiration;
    protected String currentAuthToken;

    public boolean isTokenTTLExpired() {
        if (tokenExpiration == null) return true;

        boolean result = true;
        Calendar now = Calendar.getInstance();
        long timeDiffInMillis = now.getTimeInMillis() - tokenExpiration.getTimeInMillis();
        if (timeDiffInMillis < -2000L) {
            // token will be valid for at least another 2s
            result = false;
            LOGGER.log(Level.FINE, "Auth token is still valid");
        } else {
            LOGGER.log(Level.FINE, "Auth token has to be re-issued" + timeDiffInMillis);
        }

        return result;
    }

    public void getTTLExpiryOfCurrentToken(Vault vault) {
        int tokenTTL = 0;

        try {
            // save token TTL
            tokenTTL = (int)vault.auth().lookupSelf().getTTL();
        } catch (VaultException e) {
            LOGGER.log(Level.WARNING, "Could not determine token expiration. " +
                    "Check if token is allowed to access auth/token/lookup-self. " +
                    "Assuming token TTL expired.", e);
        }

        tokenExpiration = Calendar.getInstance();
        tokenExpiration.add(Calendar.SECOND, tokenTTL);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VaultAuthenticatorWithExpiration authenticator = (VaultAuthenticatorWithExpiration) o;
        return hashCode() == authenticator.hashCode();
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
