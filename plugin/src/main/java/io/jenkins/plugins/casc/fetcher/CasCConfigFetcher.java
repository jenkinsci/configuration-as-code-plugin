package io.jenkins.plugins.casc.fetcher;

import hudson.ExtensionPoint;
import java.io.IOException;

public interface CasCConfigFetcher extends ExtensionPoint {

    boolean supports(String location);

    FetchResult fetch(String location, FetchCredentials credentials) throws IOException;
}
