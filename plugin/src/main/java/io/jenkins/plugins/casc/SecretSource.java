package io.jenkins.plugins.casc;

import hudson.ExtensionPoint;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Resolves variable references in configuration file of the form "${abc}"
 *
 * <p>
 * Variable references are meant to hide secrets from configuration files.
 */

public abstract class SecretSource implements ExtensionPoint {

    public void init() {
        // NOOP
    }

    public abstract Optional<String> reveal(String secret) throws IOException;

    public static List<SecretSource> all() {
        List<SecretSource> all = new ArrayList<>();
        all.addAll(Jenkins.getInstance().getExtensionList(SecretSource.class));
        return all;
    }
}
