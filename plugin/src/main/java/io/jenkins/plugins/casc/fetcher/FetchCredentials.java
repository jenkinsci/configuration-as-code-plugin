package io.jenkins.plugins.casc.fetcher;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import jenkins.model.Jenkins;

public interface FetchCredentials {

    @CheckForNull
    <T extends FetchAuthData> T get(String credentialId, Class<T> type);

    static FetchCredentials resolveAll() {
        return new FetchCredentials() {
            @Override
            public <T extends FetchAuthData> T get(String credentialId, Class<T> type) {
                Jenkins jenkins = Jenkins.getInstanceOrNull();
                if (jenkins != null) {
                    try {
                        for (FetchCredentialsProvider provider :
                                jenkins.getExtensionList(FetchCredentialsProvider.class)) {
                            T authData = provider.getCredentials(credentialId, type);
                            if (authData != null) {
                                return authData;
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }

                return BootstrapEnvVarCredentialResolver.INSTANCE.resolve(credentialId, type);
            }
        };
    }
}
