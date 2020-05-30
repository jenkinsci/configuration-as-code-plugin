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

    /**
     * Reveal the plaintext value of a secret.
     *
     * @param secret the variable reference to reveal
     *
     * @return the secret's value, or Optional.empty() if a recoverable error occurred. (An empty Optional will allow CasC to continue processing the resolver chain.)
     * <p>Recoverable errors include:</p>
     * <ul>
     * <li>the secret was not found in the backing store</li>
     * </ul>
     *
     * @throws IOException if an unrecoverable error occurred. (The exception will stop CasC processing the resolver chain.)
     * <p>Unrecoverable errors include:</p>
     * <ul>
     * <li>all attempts to contact the backing store have failed (including any applicable retry strategies)</li>
     * <li>authentication or authorization with the backing store failed</li>
     * <li>the secret's value was not convertible to a String</li>
     * </ul>
     */
    public abstract Optional<String> reveal(String secret) throws IOException;

    public static List<SecretSource> all() {
        return new ArrayList<>(
            Jenkins.get().getExtensionList(SecretSource.class));
    }
}
