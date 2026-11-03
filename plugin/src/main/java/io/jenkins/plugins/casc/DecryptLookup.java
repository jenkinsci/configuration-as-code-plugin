package io.jenkins.plugins.casc;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jenkins.plugins.casc.encryption.DecryptionEngine;
import org.apache.commons.text.lookup.StringLookup;

class DecryptLookup implements StringLookup {

    static final DecryptLookup INSTANCE = new DecryptLookup();

    private DecryptLookup() {}

    @Override
    public String lookup(@NonNull final String key) {
        String[] parts = key.split(":", 2);

        if (parts.length < 2 || parts[0].isBlank() || parts[1].isEmpty()) {
            throw new IllegalArgumentException("Invalid decrypt syntax. Expected format: ${decrypt:engineId:payload}");
        }

        String engineId = parts[0];
        String payload = parts[1];

        for (DecryptionEngine engine : DecryptionEngine.all()) {
            if (engine.getId().equalsIgnoreCase(engineId)) {
                String decrypted;
                try {
                    decrypted = engine.decrypt(payload);
                } catch (Exception e) {
                    throw new IllegalStateException(
                            "Failed to decrypt secret using engine '" + engineId + "' due to "
                                    + e.getClass().getSimpleName(),
                            e);
                }
                if (decrypted == null) {
                    throw new IllegalStateException("Decryption engine '" + engineId + "' returned null.");
                }
                return decrypted;
            }
        }

        throw new IllegalStateException("No DecryptionEngine found matching ID: " + engineId);
    }
}
