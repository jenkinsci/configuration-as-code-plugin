package io.jenkins.plugins.casc.fetcher;

public final class BootstrapEnvVarCredentialResolver {

    public static final BootstrapEnvVarCredentialResolver INSTANCE = new BootstrapEnvVarCredentialResolver();

    private BootstrapEnvVarCredentialResolver() {}

    @SuppressWarnings("unchecked")
    public <T extends FetchAuthData> T resolve(String credentialId, Class<T> type) {
        if (type == FetchAuthData.Token.class) {
            String token = System.getenv(credentialId);
            if (token != null && !token.isEmpty()) {
                return (T) (FetchAuthData.Token) () -> token;
            }
        }

        if (type == FetchAuthData.SshKey.class) {
            String privateKey = System.getenv(credentialId + "_PRIVATE_KEY");
            if (privateKey != null && !privateKey.isEmpty()) {
                String username = System.getenv(credentialId + "_USERNAME");
                String passphrase = System.getenv(credentialId + "_PASSPHRASE");
                return (T) new FetchAuthData.SshKey() {
                    @Override
                    public String getUsername() {
                        return username != null ? username : "git";
                    }

                    @Override
                    public String getPrivateKey() {
                        return privateKey;
                    }

                    @Override
                    public String getPassphrase() {
                        return passphrase;
                    }
                };
            }
        }
        return null;
    }
}
