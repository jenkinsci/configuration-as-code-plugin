package io.jenkins.plugins.casc.impl;

import hudson.Extension;
import io.jenkins.plugins.casc.encryption.EncryptionEngine;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

/**
 * Provides RSA decryption for short secrets (e.g., passwords, tokens).
 * Payload Limits: Pure RSA has strict size limits. For a 2048-bit key using
 * OAEP with SHA-256 padding, the maximum plaintext size is 190 bytes.
 * This engine cannot be used for large secrets like SSH private keys.
 * Format Requirement: Supports only unencrypted PKCS#8 RSA private keys in PEM format.
 * Lifecycle: The parsed private key is cached for the lifetime of this engine.
 * If the key file is rotated on disk, Jenkins must be restarted or the plugin
 * reloaded to instantiate a fresh engine.
 */
@Extension
public class RSAEncryptionEngine extends EncryptionEngine {

    public static final String KEY_ENV = "CASC_DECRYPTION_KEY_FILE";
    public static final String KEY_PROP = "casc.decryption.key.file";

    // A standard PEM-encoded 4096-bit RSA key is ~3.2KB.
    // 16KB is an upper bound that prevents memory exhaustion from misconfiguration.
    private static final long MAX_KEY_FILE_SIZE_BYTES = 16384;

    private PrivateKey cachedPrivateKey;

    @Override
    public String getId() {
        return "rsa";
    }

    @Override
    public String decrypt(String encryptedData) throws GeneralSecurityException {
        PrivateKey privateKey = getPrivateKey();

        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");

        OAEPParameterSpec oaepParams =
                new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);

        cipher.init(Cipher.DECRYPT_MODE, privateKey, oaepParams);

        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    private synchronized PrivateKey getPrivateKey() {
        if (cachedPrivateKey != null) {
            return cachedPrivateKey;
        }

        String keyPath = System.getProperty(KEY_PROP, System.getenv(KEY_ENV));
        if (keyPath == null || keyPath.isBlank()) {
            throw new IllegalStateException("Decryption key file environment variable or property is not configured.");
        }

        File keyFile = new File(keyPath);
        if (!keyFile.exists() || !keyFile.canRead()) {
            throw new IllegalStateException("Decryption key file does not exist or lacks read permissions.");
        }

        if (keyFile.length() > MAX_KEY_FILE_SIZE_BYTES) {
            throw new IllegalStateException(
                "Decryption key file exceeds maximum allowed size (16KB). Check configuration.");
        }

        try {
            byte[] keyBytes = Files.readAllBytes(keyFile.toPath());
            String keyPem = new String(keyBytes, StandardCharsets.UTF_8).trim();

            cachedPrivateKey = parsePrivateKey(keyPem);
            return cachedPrivateKey;

        } catch (IOException e) {
            throw new IllegalStateException("I/O error occurred while reading the decryption key file.", e);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException(
                "Failed to parse the PKCS#8 RSA private key. Ensure it is a valid, unencrypted PKCS#8 PEM.", e);
        }
    }

    private PrivateKey parsePrivateKey(String keyPem) throws GeneralSecurityException {
        if (!keyPem.startsWith("-----BEGIN PRIVATE KEY-----")) {
            if (keyPem.startsWith("-----BEGIN RSA PRIVATE KEY-----")) {
                throw new IllegalArgumentException(
                    "Traditional PKCS#1 RSA keys are not supported. Please convert to PKCS#8 format.");
            }
            throw new IllegalArgumentException("Invalid key format. Expected unencrypted PKCS#8 PEM format.");
        }

        String base64KeyData = keyPem.replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s+", "");

        byte[] decoded = Base64.getDecoder().decode(base64KeyData);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");

        PrivateKey parsedKey = kf.generatePrivate(spec);

        if (!(parsedKey instanceof RSAPrivateKey rsaKey)) {
            throw new IllegalArgumentException("The provided PKCS#8 key is not an RSA key.");
        }
        if (rsaKey.getModulus().bitLength() < 2048) {
            throw new IllegalArgumentException("RSA key size must be at least 2048 bits. Weak keys are rejected.");
        }

        return rsaKey;
    }
}
