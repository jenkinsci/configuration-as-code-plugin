package io.jenkins.plugins.casc;

import static com.nimbusds.jose.EncryptionMethod.A256GCM;
import static com.nimbusds.jose.JWEAlgorithm.RSA_OAEP_256;
import static io.jenkins.plugins.casc.impl.JweEncryptionEngine.KEY_PROP;
import static java.lang.System.clearProperty;
import static java.lang.System.getProperty;
import static java.lang.System.setProperty;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.Files.writeString;
import static java.util.Base64.getMimeEncoder;
import static java.util.Base64.getUrlEncoder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSAEncrypter;
import io.jenkins.plugins.casc.encryption.DecryptionEngine;
import io.jenkins.plugins.casc.impl.JweEncryptionEngine;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class DecryptLookupTest {

    private KeyPair keyPair;
    private Path keyFile;
    private String originalKeyProperty;

    @BeforeEach
    void setUp() throws Exception {
        originalKeyProperty = getProperty(KEY_PROP);

        keyPair = generateKeyPair();
        keyFile = createTempFile("casc-decrypt-lookup", ".pem");
        writePrivateKey(keyFile, keyPair.getPrivate());

        setProperty(KEY_PROP, keyFile.toAbsolutePath().toString());

        // Clear the cached private keys so the engine re-reads the key file
        clearRsaEngineCache();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (originalKeyProperty == null) {
            clearProperty(KEY_PROP);
        } else {
            setProperty(KEY_PROP, originalKeyProperty);
        }

        clearRsaEngineCache();

        if (keyFile != null) {
            deleteIfExists(keyFile);
        }
    }

    private static void clearRsaEngineCache() {
        for (DecryptionEngine engine : DecryptionEngine.all()) {
            if (engine instanceof JweEncryptionEngine rsaEngine) {
                try {
                    Field f = JweEncryptionEngine.class.getDeclaredField("cachedPrivateKeys");
                    f.setAccessible(true);
                    f.set(rsaEngine, null);
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException("Could not clear JweEncryptionEngine cache", e);
                }
            }
        }
    }

    @Test
    @SuppressWarnings({"unused"})
    void lookupDecryptsPayloadUsingMatchingEngine(JenkinsRule j) throws Exception {
        String secret = "lookup-test-secret";
        String kid = computeKeyId(keyPair.getPrivate());
        String jwePayload = encryptJwe(secret, keyPair.getPublic(), kid);

        String decrypted = DecryptLookup.INSTANCE.lookup("jwe:" + jwePayload);

        assertThat(decrypted, equalTo(secret));
    }

    @Test
    @SuppressWarnings({"unused"})
    void lookupRejectsUnknownEngine(JenkinsRule j) {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class, () -> DecryptLookup.INSTANCE.lookup("unknown-engine:payload"));

        assertThat(exception.getMessage(), containsString("unknown-engine"));
    }

    @Test
    @SuppressWarnings({"unused"})
    void lookupRejectsMissingEnginePayloadSeparator(JenkinsRule j) {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> DecryptLookup.INSTANCE.lookup("jwe"));

        assertThat(exception.getMessage(), containsString("Invalid decrypt syntax"));
    }

    @Test
    @SuppressWarnings({"unused"})
    void lookupIncludesUnderlyingDecryptionFailureMessage(JenkinsRule j) {
        String ciphertext = "invalid.jwe.payload";

        IllegalStateException exception =
                assertThrows(IllegalStateException.class, () -> DecryptLookup.INSTANCE.lookup("jwe:" + ciphertext));

        assertThat(exception.getMessage(), containsString("Failed to parse JWE payload"));

        assertThat(exception.getCause(), notNullValue());
    }

    @Test
    @SuppressWarnings("unused")
    void lookupUsesFallbackMessageWhenDecryptionExceptionHasNullMessage(JenkinsRule j) {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class, () -> DecryptLookup.INSTANCE.lookup("null-message-test:payload"));

        assertThat(exception.getMessage(), containsString("An unexpected error occurred."));
        assertThat(exception.getCause(), notNullValue());
    }

    @TestExtension
    public static class NullMessageDecryptionEngine extends DecryptionEngine {

        @Override
        public String getId() {
            return "null-message-test";
        }

        @Override
        public String decrypt(String encryptedData) throws GeneralSecurityException {
            throw new GeneralSecurityException((String) null);
        }
    }

    private static KeyPair generateKeyPair() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static String encryptJwe(String plaintext, PublicKey publicKey, String kid) throws Exception {
        JWEHeader header =
                new JWEHeader.Builder(RSA_OAEP_256, A256GCM).keyID(kid).build();

        JWEObject jwe = new JWEObject(header, new Payload(plaintext));
        jwe.encrypt(new RSAEncrypter((RSAPublicKey) publicKey));

        return jwe.serialize();
    }

    private static void writePrivateKey(Path path, PrivateKey privateKey) throws IOException {
        String encodedKey = getMimeEncoder(64, new byte[] {'\n'}).encodeToString(privateKey.getEncoded());
        String pem = "-----BEGIN PRIVATE KEY-----\n" + encodedKey + "\n-----END PRIVATE KEY-----\n";
        writeString(path, pem, UTF_8);
    }

    private static String computeKeyId(PrivateKey privateKey) throws GeneralSecurityException {
        RSAPrivateCrtKey key = (RSAPrivateCrtKey) privateKey;
        RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(key.getModulus(), key.getPublicExponent());
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(publicKey.getEncoded());
        byte[] truncated = Arrays.copyOf(hash, 8);

        return getUrlEncoder().withoutPadding().encodeToString(truncated);
    }
}
