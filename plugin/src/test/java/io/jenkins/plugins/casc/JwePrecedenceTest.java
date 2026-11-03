package io.jenkins.plugins.casc;

import static io.jenkins.plugins.casc.impl.JweEncryptionEngine.KEY_ENV;
import static io.jenkins.plugins.casc.impl.JweEncryptionEngine.KEY_PROP;
import static java.lang.System.clearProperty;
import static java.lang.System.getProperty;
import static java.lang.System.setProperty;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.deleteIfExists;
import static java.util.Base64.getUrlEncoder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSAEncrypter;
import io.jenkins.plugins.casc.impl.JweEncryptionEngine;
import java.io.IOException;
import java.nio.file.Files;
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
import java.util.Base64;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@ExtendWith(SystemStubsExtension.class)
class JwePrecedenceTest {

    @SystemStub
    private final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    private Path propKeyFile;
    private Path envKeyFile;
    private KeyPair propKeyPair;
    private KeyPair envKeyPair;
    private String originalProp;

    @BeforeEach
    void setUp() throws Exception {
        originalProp = getProperty(KEY_PROP);

        propKeyPair = generateKeyPair();
        envKeyPair = generateKeyPair();

        propKeyFile = createTempFile("casc-prop-key", ".pem");
        envKeyFile = createTempFile("casc-env-key", ".pem");

        writePrivateKey(propKeyFile, propKeyPair.getPrivate());
        writePrivateKey(envKeyFile, envKeyPair.getPrivate());
    }

    @AfterEach
    void tearDown() throws IOException {
        if (originalProp == null) {
            clearProperty(KEY_PROP);
        } else {
            setProperty(KEY_PROP, originalProp);
        }

        deleteIfExists(propKeyFile);
        deleteIfExists(envKeyFile);
    }

    @Test
    void systemPropertyTakesPrecedenceOverEnvironment() throws Exception {
        environmentVariables.set(KEY_ENV, envKeyFile.toAbsolutePath().toString());

        setProperty(KEY_PROP, propKeyFile.toAbsolutePath().toString());
        JweEncryptionEngine engine = new JweEncryptionEngine();

        String propKid = computeKeyId(propKeyPair.getPrivate());
        String propJwe = encrypt("secret-from-prop-key", propKeyPair.getPublic(), propKid);

        assertEquals("secret-from-prop-key", engine.decrypt(propJwe));

        String envKid = computeKeyId(envKeyPair.getPrivate());
        String envJwe = encrypt("secret-from-env-key", envKeyPair.getPublic(), envKid);

        assertThrows(GeneralSecurityException.class, () -> engine.decrypt(envJwe));
    }

    @Test
    void environmentVariableIsUsedWhenSystemPropertyIsNotSet() throws Exception {
        clearProperty(KEY_PROP);

        environmentVariables.set(KEY_ENV, envKeyFile.toAbsolutePath().toString());

        JweEncryptionEngine engine = new JweEncryptionEngine();

        String envKid = computeKeyId(envKeyPair.getPrivate());
        String envJwe = encrypt("secret-from-env-key", envKeyPair.getPublic(), envKid);

        assertEquals("secret-from-env-key", engine.decrypt(envJwe));
    }

    @Test
    void decryptionFailsWhenNoKeyConfigurationIsProvided() throws Exception {
        clearProperty(KEY_PROP);
        environmentVariables.set(KEY_ENV, null);

        JweEncryptionEngine engine = new JweEncryptionEngine();

        String dummyKid = computeKeyId(propKeyPair.getPrivate());
        String dummyJwe = encrypt("secret-with-missing-config", propKeyPair.getPublic(), dummyKid);

        assertThrows(IllegalStateException.class, () -> engine.decrypt(dummyJwe));
    }

    private static KeyPair generateKeyPair() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static String encrypt(String plaintext, PublicKey publicKey, String kid) throws Exception {
        JWEHeader header = new JWEHeader.Builder(JWEAlgorithm.RSA_OAEP_256, EncryptionMethod.A256GCM)
                .keyID(kid)
                .build();
        JWEObject jwe = new JWEObject(header, new Payload(plaintext));
        jwe.encrypt(new RSAEncrypter((RSAPublicKey) publicKey));
        return jwe.serialize();
    }

    private static void writePrivateKey(Path path, PrivateKey privateKey) throws IOException {
        String encodedKey = Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(privateKey.getEncoded());
        String pem = "-----BEGIN PRIVATE KEY-----\n" + encodedKey + "\n-----END PRIVATE KEY-----\n";
        Files.writeString(path, pem, UTF_8);
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
