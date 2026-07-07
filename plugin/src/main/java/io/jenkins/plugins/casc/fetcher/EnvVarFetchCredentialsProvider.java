package io.jenkins.plugins.casc.fetcher;

import hudson.Extension;

@Extension(ordinal = -100)
public class EnvVarFetchCredentialsProvider implements FetchCredentialsProvider {

    @Override
    public <T extends FetchAuthData> T getCredentials(String credentialId, Class<T> type) {
        return BootstrapEnvVarCredentialResolver.INSTANCE.resolve(credentialId, type);
    }
}
