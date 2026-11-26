package io.jenkins.plugins.casc.encryption;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Extension point for decrypting externally encrypted configuration values.
 *
 * <p>Each implementation must provide a unique, non-empty engine ID.
 * Engine IDs are matched case-insensitively.
 *
 * <p>Implementations must be thread-safe because an engine may be invoked
 * concurrently during configuration processing.
 *
 * <p>Decryption failures must be reported through exceptions. Implementations
 * must never return {@code null} or include decrypted plaintext, private keys,
 * or other sensitive material in exception messages.
 *
 * @since TODO
 */
public abstract class DecryptionEngine implements ExtensionPoint {

    /**
     * Returns the unique identifier of this decryption engine.
     *
     * @return the engine identifier
     */
    public abstract String getId();

    /**
     * Decrypts the supplied encrypted payload.
     * Implementations must return the decrypted plaintext or throw an
     * exception if decryption fails. This method must never return {@code null}.
     * Exception messages must not disclose plaintext, private keys,
     * or other sensitive information.
     *
     * @param encryptedData the encrypted payload
     * @return the decrypted plaintext
     * @throws GeneralSecurityException if decryption fails
     * @throws IOException if an I/O error occurs
     */
    public abstract String decrypt(String encryptedData) throws GeneralSecurityException, IOException;

    /**
     * Returns all registered decryption engines.
     * Fails if multiple extensions declare the same engine ID,
     * ignoring case.
     *
     * @return all registered decryption engines
     * @throws IllegalStateException if duplicate or invalid engine IDs exist
     */
    public static ExtensionList<DecryptionEngine> all() {
        ExtensionList<DecryptionEngine> engines = ExtensionList.lookup(DecryptionEngine.class);

        Map<String, DecryptionEngine> registeredIds = new HashMap<>();

        for (DecryptionEngine engine : engines) {
            String id = engine.getId();

            if (id == null || id.isBlank()) {
                throw new IllegalStateException(
                        "Decryption engine " + engine.getClass().getName() + " has a null or blank ID.");
            }

            String normalizedId = id.toLowerCase(Locale.ROOT);
            DecryptionEngine existing = registeredIds.putIfAbsent(normalizedId, engine);

            if (existing != null) {
                throw new IllegalStateException("Duplicate decryption engine ID '" + id + "' declared by "
                        + existing.getClass().getName() + " and "
                        + engine.getClass().getName() + ".");
            }
        }

        return engines;
    }
}
