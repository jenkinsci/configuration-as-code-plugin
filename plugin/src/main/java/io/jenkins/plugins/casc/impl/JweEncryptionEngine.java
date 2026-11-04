package io.jenkins.plugins.casc.impl;

import static com.nimbusds.jose.EncryptionMethod.A256GCM;
import static com.nimbusds.jose.JWEAlgorithm.RSA_OAEP_256;
import static java.nio.ByteBuffer.wrap;
import static java.nio.charset.CodingErrorAction.REPORT;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.copyOf;
import static java.util.Base64.getDecoder;
import static java.util.Base64.getUrlEncoder;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.crypto.RSADecrypter;
import hudson.Extension;
import io.jenkins.plugins.casc.encryption.DecryptionEngine;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Provides hybrid encryption decryption using JSON Web Encryption (JWE).
 * <p>
 * Uses RSA-OAEP-256 to unwrap a randomly generated content-encryption key
 * and AES-256-GCM to decrypt the payload. This supports larger secrets
 * without the plaintext size restrictions of direct RSA encryption.
 * <p>
 * Format Requirement: Accepts JWE Compact Serialization with
 * RSA-OAEP-256, A256GCM, and a key ID in the protected header.
 * Private keys must be unencrypted PKCS#8 RSA PEM blocks concatenated
 * in a single file.
 * <p>
 * Key Lifecycle: Parsed private keys are cached. The SHA-256 hash of
 * the key file contents is checked during each decryption, and keys
 * are reloaded when the file contents change.
 * <p>
 * Security Considerations: Decryption occurs during JCasC variable
 * interpolation and is not restricted to fields of type
 * {@code hudson.util.Secret}. Anyone who can modify JCasC configuration
 * must therefore be trusted with access to all decryptable secrets.
 * <p>
 * This engine returns UTF-8 strings. Binary secrets must be encoded
 * (for example, using Base64) before encryption.
 */
@Extension
public class JweEncryptionEngine extends DecryptionEngine {

    public static final String KEY_ENV = "CASC_DECRYPTION_KEY_FILE";
    public static final String KEY_PROP = "casc.decryption.key.file";

    // Standard expected header parameters for JCasC JWE compact serialization.
    private static final Set<String> ALLOWED_HEADER_PARAMS = Set.of("alg", "enc", "kid", "typ", "cty");

    // A standard PEM-encoded 4096-bit RSA key is ~3.2KB.
    // 64KB allows for ~20 concurrent keys in a file to easily support rotation.
    private static final long MAX_KEY_FILE_SIZE_BYTES = 65536;

    // 10 MB character limit to prevent resource exhaustion during parsing/decryption.
    private static final int MAX_JWE_SIZE_BYTES = 10 * 1024 * 1024;

    // Key IDs are Base64URL-encoded 8-byte SHA-256 prefixes, producing exactly 11 characters.
    private static final int MAX_KID_LENGTH = 11;

    private Map<String, PrivateKey> cachedPrivateKeys;
    private byte[] cachedContentHash;

    @Override
    public String getId() {
        return "jwe";
    }

    @Override
    public String decrypt(String encryptedData) throws GeneralSecurityException, IOException {
        if (encryptedData == null || encryptedData.isBlank()) {
            throw new IllegalArgumentException("JWE payload must not be empty.");
        }

        if (encryptedData.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("JWE payload must not contain whitespace. "
                    + "Ensure it uses a single-line JWE Compact Serialization.");
        }

        if (encryptedData.length() > MAX_JWE_SIZE_BYTES) {
            throw new IllegalArgumentException("JWE payload exceeds the maximum allowed size.");
        }

        try {
            JWEObject jweObject = JWEObject.parse(encryptedData);
            String headerKid = validateHeaderAndGetKid(jweObject.getHeader());

            Map<String, PrivateKey> keys = getPrivateKeys();
            PrivateKey privateKey = keys.get(headerKid);

            if (privateKey == null) {
                throw new GeneralSecurityException("No private key found matching the JWE key ID '" + headerKid + "'. "
                        + "Available key IDs are: " + keys.keySet() + ". "
                        + "Verify that the configured decryption key file contains "
                        + "the corresponding private key.");
            }

            jweObject.decrypt(new RSADecrypter(privateKey));

            CharsetDecoder decoder = UTF_8.newDecoder().onMalformedInput(REPORT).onUnmappableCharacter(REPORT);

            try {
                return decoder.decode(wrap(jweObject.getPayload().toBytes())).toString();
            } catch (CharacterCodingException e) {
                throw new GeneralSecurityException("Decrypted JWE payload is not valid UTF-8.", e);
            }

        } catch (ParseException e) {
            throw new IllegalArgumentException(
                    "Failed to parse JWE payload. Ensure it uses JWE compact serialization.", e);
        } catch (JOSEException e) {
            throw new GeneralSecurityException(
                    "JWE decryption failed. Verify that the ciphertext was encrypted "
                            + "using the matching RSA private key, RSA-OAEP-256, and A256GCM. "
                            + "Also check that the ciphertext has not been modified.",
                    e);
        }
    }

    private String validateHeaderAndGetKid(JWEHeader header) throws GeneralSecurityException {
        if (header.getCompressionAlgorithm() != null) {
            throw new GeneralSecurityException("JWE compression is not supported.");
        }

        if (header.getCriticalParams() != null && !header.getCriticalParams().isEmpty()) {
            throw new GeneralSecurityException("Critical JWE headers are not supported.");
        }

        Set<String> includedParams = header.getIncludedParams();

        if (!includedParams.containsAll(Set.of("alg", "enc", "kid"))) {
            throw new GeneralSecurityException("JWE header must contain alg, enc, and kid.");
        }

        if (!ALLOWED_HEADER_PARAMS.containsAll(includedParams)) {
            throw new GeneralSecurityException("JWE header contains unsupported parameters. "
                    + "Allowed parameters are alg, enc, kid, typ, and cty.");
        }

        if (!RSA_OAEP_256.equals(header.getAlgorithm())) {
            throw new GeneralSecurityException("Unsupported JWE key management algorithm. Expected RSA-OAEP-256.");
        }

        if (!A256GCM.equals(header.getEncryptionMethod())) {
            throw new GeneralSecurityException("Unsupported JWE content encryption algorithm. Expected A256GCM.");
        }

        String headerKid = header.getKeyID();
        if (headerKid == null || headerKid.isBlank()) {
            throw new GeneralSecurityException("JWE protected header must contain a key ID (kid).");
        }

        if (headerKid.length() > MAX_KID_LENGTH) {
            throw new GeneralSecurityException("JWE key ID exceeds the maximum allowed length.");
        }

        if (!headerKid.chars().allMatch(c -> c >= 0x21 && c <= 0x7E)) {
            throw new GeneralSecurityException("JWE key ID contains invalid characters.");
        }

        return headerKid;
    }

    private synchronized Map<String, PrivateKey> getPrivateKeys() {
        File keyFile = resolveKeyFile();

        try (InputStream input = Files.newInputStream(keyFile.toPath())) {
            byte[] keyBytes = input.readNBytes((int) MAX_KEY_FILE_SIZE_BYTES + 1);

            if (keyBytes.length > MAX_KEY_FILE_SIZE_BYTES) {
                throw new IllegalStateException(
                        "Decryption key file exceeds maximum allowed size (64KB). Check configuration.");
            }

            byte[] newHash = MessageDigest.getInstance("SHA-256").digest(keyBytes);

            if (cachedPrivateKeys != null && Arrays.equals(newHash, cachedContentHash)) {
                return cachedPrivateKeys;
            }

            String keyPem = new String(keyBytes, UTF_8).trim();

            if (keyPem.startsWith("\uFEFF")) {
                keyPem = keyPem.substring(1);
            }

            cachedPrivateKeys = parsePrivateKeys(keyPem);
            cachedContentHash = newHash;

            return cachedPrivateKeys;

        } catch (IOException e) {
            throw new IllegalStateException("I/O error occurred while reading the decryption key file.", e);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Failed to parse the PKCS#8 RSA private keys. "
                            + "Ensure they are valid, unencrypted PKCS#8 PEM blocks. "
                            + e.getMessage(),
                    e);
        }
    }

    private File resolveKeyFile() {
        String keyPath = System.getProperty(KEY_PROP);

        if (keyPath == null || keyPath.isBlank()) {
            keyPath = System.getenv(KEY_ENV);
        }

        if (keyPath == null || keyPath.isBlank()) {
            throw new IllegalStateException("Decryption key file is not configured. Set either the system property '"
                    + KEY_PROP
                    + "' or the environment variable '"
                    + KEY_ENV
                    + "'.");
        }

        File keyFile = new File(keyPath);

        if (!keyFile.exists()) {
            throw new IllegalStateException("Decryption key file does not exist at path: " + keyFile.getAbsolutePath()
                    + ". Configured using system property '" + KEY_PROP
                    + "' or environment variable '" + KEY_ENV + "'.");
        }

        if (!keyFile.isFile()) {
            throw new IllegalStateException("Decryption key path is not a regular file: " + keyFile.getAbsolutePath());
        }

        if (!keyFile.canRead()) {
            throw new IllegalStateException(
                    "Decryption key file is not readable: " + keyFile.getAbsolutePath() + ". Check file permissions.");
        }

        return keyFile;
    }

    private Map<String, PrivateKey> parsePrivateKeys(String keyPem) throws GeneralSecurityException {
        Map<String, PrivateKey> keys = new HashMap<>();
        String remaining = keyPem.trim();

        while (!remaining.isEmpty()) {
            if (!remaining.startsWith("-----BEGIN ")) {
                throw new IllegalArgumentException("Unexpected or malformed content found outside of PEM blocks.");
            }

            int beginEndIdx = remaining.indexOf("-----", 11);
            if (beginEndIdx == -1) {
                throw new IllegalArgumentException("Malformed PEM block: missing end of BEGIN marker.");
            }

            String blockType = remaining.substring(11, beginEndIdx);
            if (blockType.equals("RSA PRIVATE KEY")) {
                throw new IllegalArgumentException(
                        "Traditional PKCS#1 RSA keys are not supported. Please convert to PKCS#8 format.");
            }
            if (!blockType.equals("PRIVATE KEY")) {
                throw new IllegalArgumentException("Unexpected PEM block type: " + blockType);
            }

            String endMarker = "-----END PRIVATE KEY-----";
            int endIdx = remaining.indexOf(endMarker, beginEndIdx);
            if (endIdx == -1) {
                throw new IllegalArgumentException("Incomplete PEM block missing end marker.");
            }

            String base64KeyData = remaining.substring(beginEndIdx + 5, endIdx).replaceAll("\\s+", "");

            byte[] decoded = getDecoder().decode(base64KeyData);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
            KeyFactory kf = KeyFactory.getInstance("RSA");

            PrivateKey parsedKey = kf.generatePrivate(spec);

            if (!(parsedKey instanceof RSAPrivateCrtKey rsaKey)) {
                throw new IllegalArgumentException("The provided PKCS#8 key is not an RSA CRT key.");
            }
            if (rsaKey.getModulus().bitLength() < 2048) {
                throw new IllegalArgumentException("RSA key size must be at least 2048 bits. Weak keys are rejected.");
            }

            String kid = computeKeyId(rsaKey);
            if (keys.putIfAbsent(kid, rsaKey) != null) {
                throw new IllegalArgumentException("Duplicate RSA key ID found: " + kid);
            }

            remaining = remaining.substring(endIdx + endMarker.length()).trim();
        }

        if (keys.isEmpty()) {
            throw new IllegalArgumentException("Invalid key format. Expected unencrypted PKCS#8 PEM format.");
        }

        return keys;
    }

    private String computeKeyId(RSAPrivateCrtKey key) throws GeneralSecurityException {
        RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(key.getModulus(), key.getPublicExponent());
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(publicKey.getEncoded());

        // Use the first 8 bytes of the SHA-256 hash to generate a compact 64-bit key identifier.
        byte[] truncated = copyOf(hash, 8);

        return getUrlEncoder().withoutPadding().encodeToString(truncated);
    }
}
