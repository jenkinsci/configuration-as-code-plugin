package io.jenkins.plugins.casc;

import static io.jenkins.plugins.casc.impl.JweEncryptionEngine.KEY_ENV;
import static io.jenkins.plugins.casc.impl.JweEncryptionEngine.KEY_PROP;
import static java.lang.System.clearProperty;
import static java.lang.System.getProperty;
import static java.lang.System.setProperty;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.createTempDirectory;
import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.Files.isReadable;
import static java.nio.file.Files.writeString;
import static java.util.Base64.getUrlEncoder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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
import org.junit.jupiter.api.io.TempDir;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@ExtendWith(SystemStubsExtension.class)
class JwePrecedenceTest {

    @SystemStub
    private final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    private Path propKeyFile;
    private Path envKeyFile;
    private Path yamlKeyFile;
    private KeyPair propKeyPair;
    private KeyPair envKeyPair;
    private KeyPair yamlKeyPair;
    private String originalProp;

    @BeforeEach
    void setUp() throws Exception {
        originalProp = getProperty(KEY_PROP);

        propKeyPair = generateKeyPair();
        envKeyPair = generateKeyPair();
        yamlKeyPair = generateKeyPair();

        propKeyFile = createTempFile("casc-prop-key", ".pem");
        envKeyFile = createTempFile("casc-env-key", ".pem");
        yamlKeyFile = createTempFile("casc-yaml-key", ".pem");

        writePrivateKey(propKeyFile, propKeyPair.getPrivate());
        writePrivateKey(envKeyFile, envKeyPair.getPrivate());
        writePrivateKey(yamlKeyFile, yamlKeyPair.getPrivate());
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
        deleteIfExists(yamlKeyFile);
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

    @Test
    void yamlKeyFileIsUsedWhenPropertyAndEnvironmentAreAbsent() throws Exception {
        clearProperty(KEY_PROP);
        environmentVariables.set(KEY_ENV, null);

        ConfigurationContext context = contextWithYamlKey(yamlKeyFile);
        JweEncryptionEngine engine = new JweEncryptionEngine();
        String yamlJwe = encryptForKeyPair("secret-from-yaml-key", yamlKeyPair);

        assertEquals("secret-from-yaml-key", engine.decrypt(yamlJwe, context));
        assertThrows(IllegalStateException.class, () -> engine.decrypt(yamlJwe));

        setProperty(KEY_PROP, "   ");
        environmentVariables.set(KEY_ENV, "");
        assertEquals("secret-from-yaml-key", engine.decrypt(yamlJwe, context));
    }

    @Test
    void systemPropertyOverridesEnvironmentAndYamlConfiguration() throws Exception {
        environmentVariables.set(KEY_ENV, envKeyFile.toAbsolutePath().toString());
        setProperty(KEY_PROP, propKeyFile.toAbsolutePath().toString());
        ConfigurationContext context = contextWithYamlKey(yamlKeyFile);

        JweEncryptionEngine engine = new JweEncryptionEngine();
        String propJwe = encryptForKeyPair("secret-from-prop-key", propKeyPair);
        String envJwe = encryptForKeyPair("secret-from-env-key", envKeyPair);
        String yamlJwe = encryptForKeyPair("secret-from-yaml-key", yamlKeyPair);

        assertEquals("secret-from-prop-key", engine.decrypt(propJwe, context));
        assertThrows(GeneralSecurityException.class, () -> engine.decrypt(envJwe, context));
        assertThrows(GeneralSecurityException.class, () -> engine.decrypt(yamlJwe, context));
    }

    @Test
    void systemPropertyOverridesYamlWhenEnvironmentIsAbsent() throws Exception {
        environmentVariables.set(KEY_ENV, null);
        setProperty(KEY_PROP, propKeyFile.toAbsolutePath().toString());
        ConfigurationContext context = contextWithYamlKey(yamlKeyFile);

        JweEncryptionEngine engine = new JweEncryptionEngine();
        String propJwe = encryptForKeyPair("secret-from-prop-key", propKeyPair);
        String yamlJwe = encryptForKeyPair("secret-from-yaml-key", yamlKeyPair);

        assertEquals("secret-from-prop-key", engine.decrypt(propJwe, context));
        assertThrows(GeneralSecurityException.class, () -> engine.decrypt(yamlJwe, context));
    }

    @Test
    void environmentVariableOverridesYamlWhenSystemPropertyIsAbsent() throws Exception {
        clearProperty(KEY_PROP);
        environmentVariables.set(KEY_ENV, envKeyFile.toAbsolutePath().toString());
        ConfigurationContext context = contextWithYamlKey(yamlKeyFile);

        JweEncryptionEngine engine = new JweEncryptionEngine();
        String envJwe = encryptForKeyPair("secret-from-env-key", envKeyPair);
        String yamlJwe = encryptForKeyPair("secret-from-yaml-key", yamlKeyPair);

        assertEquals("secret-from-env-key", engine.decrypt(envJwe, context));
        assertThrows(GeneralSecurityException.class, () -> engine.decrypt(yamlJwe, context));
    }

    @Test
    void decryptionFailsWhenContextHasNullOrBlankYamlPathAndNoOtherSource() throws Exception {
        clearProperty(KEY_PROP);
        environmentVariables.set(KEY_ENV, null);

        JweEncryptionEngine engine = new JweEncryptionEngine();
        String yamlJwe = encryptForKeyPair("secret-with-missing-config", yamlKeyPair);

        ConfigurationContext nullPathContext = new ConfigurationContext(null);
        IllegalStateException nullEx =
                assertThrows(IllegalStateException.class, () -> engine.decrypt(yamlJwe, nullPathContext));
        assertTrue(nullEx.getMessage().contains("not configured"));

        ConfigurationContext blankPathContext = new ConfigurationContext(null);
        blankPathContext.setJweKeyFileFromConfiguration("   ");
        IllegalStateException blankEx =
                assertThrows(IllegalStateException.class, () -> engine.decrypt(yamlJwe, blankPathContext));
        assertTrue(blankEx.getMessage().contains("not configured"));
    }

    @Test
    void yamlKeyFileFailsClearlyWhenInvalidMissingDirectoryOrUnreadable(@TempDir Path tempDir) throws Exception {
        clearProperty(KEY_PROP);
        environmentVariables.set(KEY_ENV, null);

        JweEncryptionEngine engine = new JweEncryptionEngine();
        String yamlJwe = encryptForKeyPair("secret", yamlKeyPair);

        Path missingFile = tempDir.resolve("does-not-exist.pem");
        IllegalStateException missingEx = assertThrows(
                IllegalStateException.class, () -> engine.decrypt(yamlJwe, contextWithYamlKey(missingFile)));
        assertTrue(missingEx.getMessage().contains("does not exist"));

        Path directory = createTempDirectory(tempDir, "key-dir");
        IllegalStateException dirEx =
                assertThrows(IllegalStateException.class, () -> engine.decrypt(yamlJwe, contextWithYamlKey(directory)));
        assertTrue(dirEx.getMessage().contains("regular file"));

        Path invalidPemFile = tempDir.resolve("invalid.pem");
        writeString(invalidPemFile, "not a valid PKCS#8 PEM block", UTF_8);
        IllegalStateException invalidEx = assertThrows(
                IllegalStateException.class, () -> engine.decrypt(yamlJwe, contextWithYamlKey(invalidPemFile)));
        assertTrue(invalidEx.getMessage().contains("Failed to parse"));

        Path unreadableFile = tempDir.resolve("unreadable.pem");
        writePrivateKey(unreadableFile, yamlKeyPair.getPrivate());
        try {
            boolean permissionsChanged = unreadableFile.toFile().setReadable(false, false);
            assumeTrue(permissionsChanged && !isReadable(unreadableFile));

            IllegalStateException unreadableEx = assertThrows(
                    IllegalStateException.class, () -> engine.decrypt(yamlJwe, contextWithYamlKey(unreadableFile)));
            assertTrue(unreadableEx.getMessage().contains("not readable"));
        } finally {
            if (!unreadableFile.toFile().setReadable(true, true)) {
                unreadableFile.toFile().deleteOnExit();
            }
        }
    }

    @Test
    void invalidHigherPrecedenceKeyDoesNotFallBackToYamlOrEnvironment(@TempDir Path tempDir) throws Exception {
        JweEncryptionEngine engine = new JweEncryptionEngine();
        String yamlJwe = encryptForKeyPair("secret-from-yaml-key", yamlKeyPair);
        ConfigurationContext validYamlContext = contextWithYamlKey(yamlKeyFile);

        Path invalidPemFile = tempDir.resolve("malformed-higher.pem");
        writeString(invalidPemFile, "malformed PEM", UTF_8);
        Path missingFile = tempDir.resolve("missing-higher.pem");

        environmentVariables.set(KEY_ENV, yamlKeyFile.toAbsolutePath().toString());
        setProperty(KEY_PROP, invalidPemFile.toAbsolutePath().toString());
        assertThrows(IllegalStateException.class, () -> engine.decrypt(yamlJwe, validYamlContext));

        setProperty(KEY_PROP, missingFile.toAbsolutePath().toString());
        assertThrows(IllegalStateException.class, () -> engine.decrypt(yamlJwe, validYamlContext));

        setProperty(KEY_PROP, propKeyFile.toAbsolutePath().toString());
        assertThrows(GeneralSecurityException.class, () -> engine.decrypt(yamlJwe, validYamlContext));

        clearProperty(KEY_PROP);
        environmentVariables.set(KEY_ENV, invalidPemFile.toAbsolutePath().toString());
        assertThrows(IllegalStateException.class, () -> engine.decrypt(yamlJwe, validYamlContext));

        environmentVariables.set(KEY_ENV, missingFile.toAbsolutePath().toString());
        assertThrows(IllegalStateException.class, () -> engine.decrypt(yamlJwe, validYamlContext));

        environmentVariables.set(KEY_ENV, envKeyFile.toAbsolutePath().toString());
        assertThrows(GeneralSecurityException.class, () -> engine.decrypt(yamlJwe, validYamlContext));
    }

    @Test
    void contextsWithDifferentYamlKeyPathsResolveIndependently(@TempDir Path tempDir) throws Exception {
        clearProperty(KEY_PROP);
        environmentVariables.set(KEY_ENV, null);

        JweEncryptionEngine engine = new JweEncryptionEngine();
        ConfigurationContext contextA = contextWithYamlKey(envKeyFile);
        ConfigurationContext contextB = contextWithYamlKey(yamlKeyFile);

        String jweForA = encryptForKeyPair("secret-for-a", envKeyPair);
        String jweForB = encryptForKeyPair("secret-for-b", yamlKeyPair);

        assertEquals("secret-for-a", engine.decrypt(jweForA, contextA));
        assertThrows(GeneralSecurityException.class, () -> engine.decrypt(jweForB, contextA));

        assertEquals("secret-for-b", engine.decrypt(jweForB, contextB));
        assertThrows(GeneralSecurityException.class, () -> engine.decrypt(jweForA, contextB));

        assertEquals("secret-for-a", engine.decrypt(jweForA, contextA));

        Path invalidPem = tempDir.resolve("bad-context-key.pem");
        writeString(invalidPem, "not a PEM key", UTF_8);
        assertThrows(IllegalStateException.class, () -> engine.decrypt(jweForA, contextWithYamlKey(invalidPem)));
    }

    @Test
    void yamlKeyPathConfiguredInOneContextDoesNotLeakIntoAnotherContext() throws Exception {
        clearProperty(KEY_PROP);
        environmentVariables.set(KEY_ENV, null);

        JweEncryptionEngine engine = new JweEncryptionEngine();
        ConfigurationContext configuredContext = contextWithYamlKey(yamlKeyFile);
        ConfigurationContext unconfiguredContext = new ConfigurationContext(null);

        String yamlJwe = encryptForKeyPair("isolated-secret", yamlKeyPair);

        assertEquals("isolated-secret", engine.decrypt(yamlJwe, configuredContext));
        IllegalStateException ex =
                assertThrows(IllegalStateException.class, () -> engine.decrypt(yamlJwe, unconfiguredContext));
        assertTrue(ex.getMessage().contains("not configured"));
    }

    private static ConfigurationContext contextWithYamlKey(Path keyPath) {
        ConfigurationContext context = new ConfigurationContext(null);
        context.setJweKeyFileFromConfiguration(keyPath.toAbsolutePath().toString());
        return context;
    }

    private static String encryptForKeyPair(String plaintext, KeyPair keyPair) throws Exception {
        String kid = computeKeyId(keyPair.getPrivate());
        return encrypt(plaintext, keyPair.getPublic(), kid);
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
