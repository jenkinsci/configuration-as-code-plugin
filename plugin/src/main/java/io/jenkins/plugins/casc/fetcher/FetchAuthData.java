package io.jenkins.plugins.casc.fetcher;

import edu.umd.cs.findbugs.annotations.CheckForNull;

public interface FetchAuthData {

    interface Token extends FetchAuthData {
        String getToken();
    }

    interface UsernamePassword extends FetchAuthData {
        String getUsername();

        String getPassword();
    }

    interface SshKey extends FetchAuthData {
        String getUsername();

        String getPrivateKey();

        @CheckForNull
        String getPassphrase();
    }
}
