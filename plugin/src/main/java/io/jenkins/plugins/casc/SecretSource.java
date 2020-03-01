package io.jenkins.plugins.casc;

import hudson.ExtensionPoint;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import jenkins.model.Jenkins;

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
        return new ArrayList<>(
            Jenkins.get().getExtensionList(SecretSource.class));
    }
}
