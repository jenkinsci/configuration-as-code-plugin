package io.jenkins.plugins.casc;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jenkins.plugins.casc.encryption.EncryptionEngine;
import org.apache.commons.text.lookup.StringLookup;

class DecryptLookup implements StringLookup {

    static final DecryptLookup INSTANCE = new DecryptLookup();

    private DecryptLookup() {}

    @Override
    public String lookup(@NonNull final String key) {
        String[] parts = key.split(":", 2);

        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid decrypt syntax. Expected format: ${decrypt:engineId:payload}");
        }

        String engineId = parts[0];
        String payload = parts[1];

        for (EncryptionEngine engine : EncryptionEngine.all()) {
            if (engine.getId().equalsIgnoreCase(engineId)) {
                try {
                    return engine.decrypt(payload);
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to decrypt secret using engine: " + engineId, e);
                }
            }
        }

        throw new IllegalStateException("No EncryptionEngine found matching ID: " + engineId);
    }
}
