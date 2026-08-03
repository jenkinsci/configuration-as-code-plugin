package io.jenkins.plugins.casc.fetcher;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.ExtensionPoint;

public interface FetchCredentialsProvider extends ExtensionPoint {

    @CheckForNull
    <T extends FetchAuthData> T getCredentials(String credentialId, Class<T> type);
}
