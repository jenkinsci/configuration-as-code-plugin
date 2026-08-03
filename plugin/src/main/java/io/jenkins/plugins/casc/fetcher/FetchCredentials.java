package io.jenkins.plugins.casc.fetcher;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;

public interface FetchCredentials {

    Logger LOGGER = Logger.getLogger(FetchCredentials.class.getName());

    @CheckForNull
    <T extends FetchAuthData> T get(String credentialId, Class<T> type);

    static FetchCredentials resolveAll() {
        return new FetchCredentials() {
            @Override
            public <T extends FetchAuthData> T get(String credentialId, Class<T> type) {
                Jenkins jenkins = Jenkins.getInstanceOrNull();
                if (jenkins != null) {
                    for (FetchCredentialsProvider provider : jenkins.getExtensionList(FetchCredentialsProvider.class)) {
                        try {
                            T authData = provider.getCredentials(credentialId, type);
                            if (authData != null) {
                                return authData;
                            }
                        } catch (Exception e) {
                            LOGGER.log(
                                    Level.FINE,
                                    e,
                                    () -> "Credential provider "
                                            + provider.getClass().getName()
                                            + " threw an exception while resolving ID: " + credentialId);
                        }
                    }
                }

                return BootstrapEnvVarCredentialResolver.INSTANCE.resolve(credentialId, type);
            }
        };
    }
}
