package io.jenkins.plugins.casc;

import static io.jenkins.plugins.casc.impl.JweEncryptionEngine.KEY_ENV;
import static io.jenkins.plugins.casc.impl.JweEncryptionEngine.KEY_PROP;
import static java.lang.System.clearProperty;
import static java.lang.System.getProperty;
import static java.lang.System.setProperty;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.createTempDirectory;
import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.Files.isReadable;
import static java.nio.file.Files.writeString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.jenkins.plugins.casc.impl.JweEncryptionEngine;
import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
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
    private JweFixture propFixture;
    private JweFixture envFixture;
    private JweFixture yamlFixture;
    private String originalProp;

    @BeforeEach
    void setUp() throws Exception {
        originalProp = getProperty(KEY_PROP);

        Path directory = createTempDirectory("casc-jwe-test");
        propFixture = new JweFixture(directory, "prop.pem");
        envFixture = new JweFixture(directory, "env.pem");
        yamlFixture = new JweFixture(directory, "yaml.pem");
        propKeyFile = propFixture.keyFile;
        envKeyFile = envFixture.keyFile;
        yamlKeyFile = yamlFixture.keyFile;
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

        String propJwe = propFixture.encrypt("secret-from-prop-key");

        assertEquals("secret-from-prop-key", engine.decrypt(propJwe, null));

        String envJwe = envFixture.encrypt("secret-from-env-key");

        assertThrows(GeneralSecurityException.class, () -> engine.decrypt(envJwe, null));
    }

    @Test
    void environmentVariableIsUsedWhenSystemPropertyIsNotSet() throws Exception {
        clearProperty(KEY_PROP);

        environmentVariables.set(KEY_ENV, envKeyFile.toAbsolutePath().toString());

        JweEncryptionEngine engine = new JweEncryptionEngine();

        String envJwe = envFixture.encrypt("secret-from-env-key");

        assertEquals("secret-from-env-key", engine.decrypt(envJwe, null));
    }

    @Test
    void decryptionFailsWhenNoKeyConfigurationIsProvided() throws Exception {
        clearProperty(KEY_PROP);
        environmentVariables.set(KEY_ENV, null);

        JweEncryptionEngine engine = new JweEncryptionEngine();

        String dummyJwe = propFixture.encrypt("secret-with-missing-config");

        assertThrows(IllegalStateException.class, () -> engine.decrypt(dummyJwe, null));
    }

    @Test
    void yamlKeyFileIsUsedWhenPropertyAndEnvironmentAreAbsent() throws Exception {
        clearProperty(KEY_PROP);
        environmentVariables.set(KEY_ENV, null);

        ConfigurationContext context = contextWithYamlKey(yamlKeyFile);
        JweEncryptionEngine engine = new JweEncryptionEngine();
        String yamlJwe = yamlFixture.encrypt("secret-from-yaml-key");

        assertEquals("secret-from-yaml-key", engine.decrypt(yamlJwe, context));
        assertThrows(IllegalStateException.class, () -> engine.decrypt(yamlJwe, null));

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
        String propJwe = propFixture.encrypt("secret-from-prop-key");
        String envJwe = envFixture.encrypt("secret-from-env-key");
        String yamlJwe = yamlFixture.encrypt("secret-from-yaml-key");

        assertEquals("secret-from-prop-key", engine.decrypt(propJwe, context));
        assertThrows(GeneralSecurityException.class, () -> engine.decrypt(envJwe, context));
        assertThrows(GeneralSecurityException.class, () -> engine.decrypt(yamlJwe, context));
    }

    @Test
    void environmentVariableOverridesYamlWhenSystemPropertyIsAbsent() throws Exception {
        clearProperty(KEY_PROP);
        environmentVariables.set(KEY_ENV, envKeyFile.toAbsolutePath().toString());
        ConfigurationContext context = contextWithYamlKey(yamlKeyFile);

        JweEncryptionEngine engine = new JweEncryptionEngine();
        String envJwe = envFixture.encrypt("secret-from-env-key");
        String yamlJwe = yamlFixture.encrypt("secret-from-yaml-key");

        assertEquals("secret-from-env-key", engine.decrypt(envJwe, context));
        assertThrows(GeneralSecurityException.class, () -> engine.decrypt(yamlJwe, context));
    }

    @Test
    void decryptionFailsWhenContextHasNullOrBlankYamlPathAndNoOtherSource() throws Exception {
        clearProperty(KEY_PROP);
        environmentVariables.set(KEY_ENV, null);

        JweEncryptionEngine engine = new JweEncryptionEngine();
        String yamlJwe = yamlFixture.encrypt("secret-with-missing-config");

        ConfigurationContext nullPathContext = new ConfigurationContext(null);
        IllegalStateException nullEx =
                assertThrows(IllegalStateException.class, () -> engine.decrypt(yamlJwe, nullPathContext));
        assertTrue(nullEx.getMessage().contains("not configured"));

        assertThrows(IllegalArgumentException.class, () -> new ConfigurationContext.JweConfiguration("   "));
    }

    @Test
    void yamlKeyFileFailsClearlyWhenInvalidMissingDirectoryOrUnreadable(@TempDir Path tempDir) throws Exception {
        clearProperty(KEY_PROP);
        environmentVariables.set(KEY_ENV, null);

        JweEncryptionEngine engine = new JweEncryptionEngine();
        String yamlJwe = yamlFixture.encrypt("secret");

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
        JweFixture.writePrivateKey(unreadableFile, yamlFixture.keyPair);
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
        String yamlJwe = yamlFixture.encrypt("secret-from-yaml-key");
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

        String jweForA = envFixture.encrypt("secret-for-a");
        String jweForB = yamlFixture.encrypt("secret-for-b");

        assertEquals("secret-for-a", engine.decrypt(jweForA, contextA));
        assertThrows(GeneralSecurityException.class, () -> engine.decrypt(jweForB, contextA));

        assertEquals("secret-for-b", engine.decrypt(jweForB, contextB));
        assertThrows(GeneralSecurityException.class, () -> engine.decrypt(jweForA, contextB));

        assertEquals("secret-for-a", engine.decrypt(jweForA, contextA));

        Path invalidPem = tempDir.resolve("bad-context-key.pem");
        writeString(invalidPem, "not a PEM key", UTF_8);
        assertThrows(IllegalStateException.class, () -> engine.decrypt(jweForA, contextWithYamlKey(invalidPem)));
    }

    private static ConfigurationContext contextWithYamlKey(Path keyPath) {
        ConfigurationContext context = new ConfigurationContext(null);
        context.getDecryption()
                .setJwe(new ConfigurationContext.JweConfiguration(
                        keyPath.toAbsolutePath().toString()));
        return context;
    }

    private static final class JweFixture {
        final KeyPair keyPair;
        final Path keyFile;
        private final String kid;

        JweFixture(Path directory, String fileName) throws Exception {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            keyPair = generator.generateKeyPair();
            keyFile = directory.resolve(fileName);
            writePrivateKey(keyFile, keyPair);
            kid = computeKeyId(keyPair);
        }

        String encrypt(String plaintext) throws Exception {
            com.nimbusds.jose.JWEHeader header = new com.nimbusds.jose.JWEHeader.Builder(
                            com.nimbusds.jose.JWEAlgorithm.RSA_OAEP_256, com.nimbusds.jose.EncryptionMethod.A256GCM)
                    .keyID(kid)
                    .build();
            com.nimbusds.jose.JWEObject jwe =
                    new com.nimbusds.jose.JWEObject(header, new com.nimbusds.jose.Payload(plaintext));
            jwe.encrypt(new com.nimbusds.jose.crypto.RSAEncrypter((RSAPublicKey) keyPair.getPublic()));
            return jwe.serialize();
        }

        static void writePrivateKey(Path path, KeyPair keyPair) throws IOException {
            String encoded = Base64.getMimeEncoder(64, new byte[] {'\n'})
                    .encodeToString(keyPair.getPrivate().getEncoded());
            writeString(path, "-----BEGIN PRIVATE KEY-----\n" + encoded + "\n-----END PRIVATE KEY-----\n", UTF_8);
        }

        private static String computeKeyId(KeyPair keyPair) throws GeneralSecurityException {
            RSAPrivateCrtKey privateKey = (RSAPrivateCrtKey) keyPair.getPrivate();
            RSAPublicKeySpec publicKeySpec =
                    new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent());
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(KeyFactory.getInstance("RSA")
                            .generatePublic(publicKeySpec)
                            .getEncoded());
            return Base64.getUrlEncoder().withoutPadding().encodeToString(Arrays.copyOf(hash, 8));
        }
    }
}
