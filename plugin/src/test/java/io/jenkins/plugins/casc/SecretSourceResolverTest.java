package io.jenkins.plugins.casc;

import static com.nimbusds.jose.EncryptionMethod.A256GCM;
import static com.nimbusds.jose.JWEAlgorithm.RSA_OAEP_256;
import static io.jenkins.plugins.casc.impl.JweEncryptionEngine.KEY_PROP;
import static java.util.Base64.getMimeEncoder;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSAEncrypter;
import io.jenkins.plugins.casc.SecretSourceResolver.Base64Lookup;
import io.jenkins.plugins.casc.SecretSourceResolver.FileBase64Lookup;
import io.jenkins.plugins.casc.SecretSourceResolver.FileStringLookup;
import io.jenkins.plugins.casc.SecretSourceResolver.SystemPropertyLookup;
import io.jenkins.plugins.casc.encryption.DecryptionEngine;
import io.jenkins.plugins.casc.impl.JweEncryptionEngine;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.text.lookup.StringLookup;
import org.apache.commons.text.lookup.StringLookupFactory;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.LoggerRule;
import org.jvnet.hudson.test.WithoutJenkins;

public class SecretSourceResolverTest {

    @ClassRule
    public static JenkinsRule j = new JenkinsRule();

    private static ConfigurationContext context;

    @Rule
    public final EnvironmentVariables environment = new EnvironmentVariables();

    @Rule
    public LoggerRule logging = new LoggerRule();

    public static final StringLookup ENCODE = Base64Lookup.INSTANCE;
    public static final StringLookup DECODE = StringLookupFactory.INSTANCE.base64DecoderStringLookup();
    public static final StringLookup FILE = FileStringLookup.INSTANCE;
    public static final StringLookup BINARYFILE = FileBase64Lookup.INSTANCE;
    public static final StringLookup SYSPROP = SystemPropertyLookup.INSTANCE;

    @Before
    public void initLogging() {
        logging.record(Logger.getLogger(SecretSourceResolver.class.getName()), Level.INFO)
                .capture(2048);
    }

    @BeforeClass
    public static void setUp() {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        context = new ConfigurationContext(registry);
    }

    public String resolve(String toInterpolate) {
        return context.getSecretSourceResolver().resolve(toInterpolate);
    }

    public boolean logContains(String text) {
        final String expectedText = text;
        return logging.getMessages().stream().anyMatch(m -> m.contains(expectedText));
    }

    public Path getPath(String fileName) throws URISyntaxException {
        return Paths.get(getClass().getResource(fileName).toURI());
    }

    @Test
    public void resolve_singleEntry() {
        environment.set("FOO", "hello");
        assertThat(resolve("${FOO}"), equalTo("hello"));
    }

    @Test
    public void resolve_singleEntryWithoutDefaultValue() {
        assertThat(resolve("${FOO}"), equalTo(""));
        assertTrue(logContains("Configuration import: Found unresolved variable 'FOO'"));
    }

    @Test
    public void resolve_singleEntryWithDefaultValue() {
        assertThat(resolve("${FOO:-default}"), equalTo("default"));
    }

    @Test
    public void resolve_singleEntryWithDefaultValueAndWithEnvDefined() {
        environment.set("FOO", "hello");
        assertThat(resolve("${FOO:-default}"), equalTo("hello"));
    }

    @Test
    public void resolve_singleEntryEscaped() {
        assertThat(resolve("^${FOO}"), equalTo("${FOO}"));
    }

    @Test
    public void resolve_singleEntryDoubleEscaped() {
        assertThat(resolve("^^${FOO}"), equalTo("^${FOO}"));
    }

    @Test
    public void resolve_multipleEntries() {
        environment.set("FOO", "hello");
        environment.set("BAR", "world");
        assertThat(resolve("${FOO}:${BAR}"), equalTo("hello:world"));
    }

    @Test
    public void resolve_multipleEntriesWithoutDefaultValue() {
        assertThat(resolve("${FOO}:${BAR}"), equalTo(":"));
    }

    @Test
    public void resolve_multipleEntriesWithDefaultValue() {
        assertThat(resolve("${FOO:-hello}:${BAR:-world}"), equalTo("hello:world"));
    }

    @Test
    public void resolve_multipleEntriesWithDefaultValueAndEnvDefined() {
        environment.set("FOO", "hello");
        environment.set("BAR", "world");
        assertThat(resolve("${FOO:-default}:${BAR:-default}"), equalTo("hello:world"));
    }

    @Test
    public void resolve_multipleEntriesEscaped() {
        assertThat(resolve("^${FOO}:^${BAR}"), equalTo("${FOO}:${BAR}"));
    }

    @Test
    public void resolve_nothing() {
        assertThat(resolve("FOO"), equalTo("FOO"));
    }

    @Test
    public void resolve_empty() {
        assertThat(resolve(""), equalTo(""));
    }

    @Test
    public void resolve_blank() {
        assertThat(resolve(" "), equalTo(" "));
    }

    @Test
    public void resolve_nothingSpace() {
        assertThat(resolve("${ }"), equalTo(""));
    }

    @Test
    public void resolve_nothingBrackets() {
        assertThat(resolve("${}"), equalTo(""));
    }

    @Test
    public void resolve_nothingDefault() {
        assertThat(resolve("${:-default}"), equalTo("default"));
    }

    @Test
    public void resolve_emptyDefault() {
        assertThat(resolve("${FOO:-}"), equalTo(""));
    }

    @Test
    public void resolve_emptyDefaultEnvDefined() {
        environment.set("FOO", "foo");
        assertThat(resolve("${FOO:-}"), equalTo("foo"));
    }

    @Test
    public void resolve_defaultValueLimit() {
        assertThat(resolve("${FOO:-default:-other}"), equalTo("default:-other"));
    }

    @Test
    public void resolve_mixedSingleEntry() {
        environment.set("FOO", "www.foo.io");
        assertThat(resolve("http://${FOO}"), equalTo("http://www.foo.io"));
    }

    @Test
    public void resolve_mixedSingleEntryWithDefault() {
        environment.set("FOO", "www.foo.io");
        assertThat(resolve("${protocol:-https}://${FOO:-www.bar.io}"), equalTo("https://www.foo.io"));
    }

    @Test
    public void resolve_mixedSingleEntryEscaped() {
        assertThat(resolve("http://^${FOO}"), equalTo("http://${FOO}"));
    }

    @Test
    public void resolve_mixedMultipleEntries() {
        environment.set("FOO", "www.foo.io");
        environment.set("BAR", "8080");
        assertThat(resolve("http://${FOO}:${BAR}"), equalTo("http://www.foo.io:8080"));
    }

    /**
     * Ensures that prefixes in the secret ID which are not covered by the CasC protocol substitutors (e.g. base64:, file:)
     * are left untouched and not removed.
     */
    @Test
    public void resolve_preservesPrefixesNotCoveredBySubstitutors() {
        final String secret = "foobar";

        environment.set("arn:aws:secretsmanager:eu-central-1:123456789012:secret:my-secret", secret);
        assertThat(resolve("${arn:aws:secretsmanager:eu-central-1:123456789012:secret:my-secret}"), equalTo(secret));
    }

    @Test
    public void resolve_mixedMultipleEntriesWithDefault() {
        environment.set("FOO", "www.foo.io");
        environment.set("protocol", "http");
        assertThat(resolve("${protocol:-https}://${FOO:-www.bar.io}"), equalTo("http://www.foo.io"));
    }

    @Test
    public void resolve_noInterpolatorStringLookupForARegularFileOrBase64Variable() {
        environment.set("base64", "foo");
        environment.set("file", "foo");
        assertThat(resolve("${base64}"), equalTo("foo"));
        assertThat(resolve("${file}"), equalTo("foo"));
    }

    @Test
    public void resolve_mixedMultipleEntriesEscaped() {
        assertThat(resolve("http://^${FOO}:^${BAR}"), equalTo("http://${FOO}:${BAR}"));
    }

    @Test
    public void resolve_Base64() {
        String input = "Hello World";
        String output = resolve("${base64:" + input + "}");
        assertThat(output, equalTo(ENCODE.lookup(input)));
        assertThat(DECODE.lookup(output), equalTo(input));
    }

    @Test
    public void resolve_Base64NestedEnv() {
        String input = "Hello World";
        environment.set("FOO", input);
        String output = resolve("${base64:${FOO}}");
        assertThat(output, equalTo(ENCODE.lookup(input)));
        assertThat(DECODE.lookup(output), equalTo(input));
    }

    @Test
    public void resolve_File() throws Exception {
        String input = getPath("secret.json").toAbsolutePath().toString();
        String output = resolve("${readFile:" + input + "}");
        assertThat(output, equalTo(FILE.lookup(input)));
        assertThat(output, containsString("\"Our secret\": \"Hello World\""));
    }

    @Test
    public void resolve_FileWithRelative() throws Exception {
        Path path = getPath("secret.json");
        String input = Paths.get("").toUri().relativize(path.toUri()).getPath();
        String output = resolve("${readFile:" + input + "}");
        assertThat(output, equalTo(FILE.lookup(input)));
        assertThat(output, containsString("\"Our secret\": \"Hello World\""));
    }

    @Test
    public void resolve_FileKey() throws Exception {
        Path path = getPath("secret.key");
        String input = Paths.get("").toUri().relativize(path.toUri()).getPath();
        String output = resolve("${readFile:" + input + "}");
        assertThat(output, equalTo(FILE.lookup(input)));
        assertThat(output, startsWith("-----BEGIN RSA PRIVATE KEY-----"));
    }

    @Test
    public void resolve_FileWithSpace() throws Exception {
        String path = getPath("some secret.json").toAbsolutePath().toString();
        String output = resolve("${readFile:" + path + "}");
        assertThat(output, equalTo(FILE.lookup(path)));
        assertThat(output, containsString("\"Our secret\": \"Hello World\""));
    }

    @Test
    public void resolve_FileWithSpaceAndRelative() throws Exception {
        String path = getPath("some secret.json").toAbsolutePath().toString();
        String input = Paths.get("").toUri().relativize(new File(path).toURI()).getPath();
        String output = resolve("${readFile:" + input + "}");
        assertThat(output, equalTo(FILE.lookup(input)));
        assertThat(output, containsString("\"Our secret\": \"Hello World\""));
    }

    @Test
    public void resolve_FileBase64() throws Exception {
        String input = getPath("secret.json").toAbsolutePath().toString();
        String output = resolve("${base64:${readFile:" + input + "}}");
        String decoded = DECODE.lookup(output);
        String content = FILE.lookup(input);
        assertThat(output, equalTo(ENCODE.lookup(content)));
        assertThat(decoded, equalTo(content));
        assertThat(decoded, containsString("\"Our secret\": \"Hello World\""));
    }

    @Test
    public void resolve_FileBase64NestedEnv() throws Exception {
        String input = getPath("secret.json").toAbsolutePath().toString();
        environment.set("FOO", input);
        String output = resolve("${base64:${readFile:${FOO}}}");
        String decoded = DECODE.lookup(output);
        String content = FILE.lookup(input);
        assertThat(output, equalTo(ENCODE.lookup(content)));
        assertThat(decoded, equalTo(content));
        assertThat(decoded, containsString("\"Our secret\": \"Hello World\""));
    }

    @Test
    public void resolve_FileNotFound() {
        resolve("${readFile:./hello-world-not-found.txt}");
        assertTrue(logContains(
                "Configuration import: Error looking up file './hello-world-not-found.txt' with UTF-8 encoding."));
        assertTrue(
                logContains("Configuration import: Found unresolved variable 'readFile:./hello-world-not-found.txt'."));
    }

    @Test
    public void resolve_FileBase64NotFound() {
        resolve("${readFileBase64:./hello-world-not-found.txt}");
        assertTrue(logContains("Configuration import: Error looking up file './hello-world-not-found.txt'."));
        assertTrue(logContains(
                "Configuration import: Found unresolved variable 'readFileBase64:./hello-world-not-found.txt'."));
    }

    @Test
    public void resolve_BinaryFileBase64() throws Exception {
        Path path = getPath("secret.json");
        String pathStr = path.toAbsolutePath().toString();
        environment.set("FOO", pathStr);
        byte[] bytes = Files.readAllBytes(path);
        String expected = Base64.getEncoder().encodeToString(bytes);
        byte[] expectedBytes = Base64.getDecoder().decode(expected);
        String actual = resolve("${fileBase64:${FOO}}");
        byte[] actualBytes = Base64.getDecoder().decode(actual);
        String lookup = BINARYFILE.lookup(pathStr);
        assertThat(lookup, equalTo(expected));
        assertThat(actual, equalTo(expected));
        assertThat(actualBytes, equalTo(expectedBytes));
    }

    @Test
    public void resolve_SystemProperty() throws Exception {
        String input = "java.version";
        String expected = System.getProperty(input);
        String output = resolve("${sysProp:" + input + "}");
        assertThat(output, equalTo(SYSPROP.lookup(input)));
        assertThat(output, equalTo(expected));
    }

    @Test
    public void resolve_SystemPropertyNotFound() throws Exception {
        String input = "java.version.non-existent";
        String expected = "";
        String output = resolve("${sysProp:" + input + "}");
        assertThat(output, equalTo(SYSPROP.lookup(input)));
        assertThat(output, equalTo(expected));
    }

    @Test
    public void resolve_Json() {
        String input = "{ \"a\": 1, \"b\": 2 }";
        environment.set("FOO", input);
        String output = resolve("${json:a:${FOO}}");
        assertThat(output, equalTo("1"));
    }

    /**
     * Check that prettified / multi-line JSON is supported
     */
    @Test
    public void resolve_JsonWithNewlineBetweenTokens() {
        String input = "{ \n \"a\": 1, \n \"b\": 2 }";
        environment.set("FOO", input);
        String output = resolve("${json:a:${FOO}}");
        assertThat(output, equalTo("1"));
    }

    @Test
    public void resolve_JsonWithNewlineInValue() {
        String input = "{ \"a\": \"hello\\nworld\", \"b\": 2 }";
        environment.set("FOO", input);
        String output = resolve("${json:a:${FOO}}");
        assertThat(output, equalTo("hello\nworld"));
    }

    @Test
    public void resolve_JsonWithSpaceInKey() {
        String input = "{ \"abc def\": 1, \"b\": 2 }";
        environment.set("FOO", input);
        String output = resolve("${json:abc def:${FOO}}");
        assertThat(output, equalTo("1"));
    }

    /**
     * Test a mix of JSON and other lookups
     */
    @Test
    public void resolve_JsonFromFile() throws Exception {
        String input = getPath("secret.json").toAbsolutePath().toString();
        String output = resolve("${json:Our secret:${file:" + input + "}}");
        assertThat(output, equalTo("Hello World"));
    }

    @Test
    public void resolve_JsonMissingKey() {
        String input = "{ \"b\": 2 }";
        environment.set("FOO", input);
        String output = resolve("${json:a:${FOO}}");
        assertTrue(
                logContains(
                        "Configuration import: JSON secret did not contain the specified key 'a'. Will default to empty string."));
        assertThat(output, equalTo(""));
    }

    @Test
    @Issue("SECURITY-1446")
    @WithoutJenkins
    public void shouldEncodeInternalVarsProperly() {
        assertVarEncoding("^${TEST}", "${TEST}");
        assertVarEncoding("^^${TEST}", "^${TEST}");
        assertVarEncoding("$TEST", "$TEST");
        assertVarEncoding(null, null);
    }

    private static void assertVarEncoding(String expected, String toEncode) {
        String encoded = context.getSecretSourceResolver().encode(toEncode);
        assertThat(encoded, equalTo(expected));
    }

    @Test
    public void trimLookup_removesTrailingWhitespace() {
        assertThat(SecretSourceResolver.TrimLookup.INSTANCE.lookup("value\n"), equalTo("value"));
        assertThat(SecretSourceResolver.TrimLookup.INSTANCE.lookup("value\r\n"), equalTo("value"));
        assertThat(SecretSourceResolver.TrimLookup.INSTANCE.lookup("  value  \n"), equalTo("  value"));
    }

    @Test
    public void resolve_trimRemovesTrailingNewlines() {
        environment.set("FOO", "my_secret_pass\n");
        assertThat(resolve("${trim:${FOO}}"), equalTo("my_secret_pass"));

        environment.set("BAR", "my_secret_pass\r\n");
        assertThat(resolve("${trim:${BAR}}"), equalTo("my_secret_pass"));

        environment.set("BAZ", "my_secret_pass\n\n\n");
        assertThat(resolve("${trim:${BAZ}}"), equalTo("my_secret_pass"));
    }

    @Test
    public void resolve_trimNoNewline_noChange() {
        environment.set("FOO", "my_secret_pass");
        assertThat(resolve("${trim:${FOO}}"), equalTo("my_secret_pass"));
    }

    @Test
    public void resolve_trimRemovesTrailingSpacesButPreservesLeading() {
        environment.set("FOO", "  my_secret_pass  \n");
        assertThat(resolve("${trim:${FOO}}"), equalTo("  my_secret_pass"));

        environment.set("BAR", "\tmy_secret_pass\t\r\n");
        assertThat(resolve("${trim:${BAR}}"), equalTo("\tmy_secret_pass"));
    }

    @Test
    public void resolve_trimNestedReadFile() throws Exception {
        Path tempSecretFile = Files.createTempFile("casc-secret", ".txt");
        tempSecretFile.toFile().deleteOnExit();

        Files.writeString(tempSecretFile, "super_secret_ssh_key\n");
        String inputPath = tempSecretFile.toAbsolutePath().toString();

        String rawOutput = resolve("${readFile:" + inputPath + "}");
        assertTrue("Raw output should contain the problematic trailing newline", rawOutput.endsWith("\n"));
        assertThat(rawOutput, equalTo("super_secret_ssh_key\n"));

        String trimmedOutput = resolve("${trim:${readFile:" + inputPath + "}}");
        assertThat(trimmedOutput, equalTo("super_secret_ssh_key"));
    }

    @Test
    public void resolve_readFilePreservesTrailingNewline() throws Exception {
        Path tempFile = Files.createTempFile("casc-secret-baseline", ".txt");
        tempFile.toFile().deleteOnExit();

        Files.writeString(tempFile, "secret\n");

        String output = resolve("${readFile:" + tempFile.toAbsolutePath() + "}");
        assertThat(output, equalTo("secret\n"));
    }

    @Test
    public void resolve_trimEmptyString() {
        assertThat(resolve("${trim:}"), equalTo(""));
    }

    @Test
    public void resolve_trimOnlyNewlines() {
        environment.set("FOO", "\n\n");
        assertThat(resolve("${trim:${FOO}}"), equalTo(""));
    }

    @Test
    public void resolve_trimWithSpacesOnly_becomesEmpty() {
        environment.set("FOO", "   ");
        assertThat(resolve("${trim:${FOO}}"), equalTo(""));
    }

    @Test
    public void resolve_strictModeEnvVar_throwsExceptionOnMissingVar() {
        environment.set("CASC_STRICT_SECRET_RESOLUTION", "true");

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            resolve("${MISSING_SECRET_VAR}");
        });

        assertThat(exception.getMessage(), containsString("MISSING_SECRET_VAR"));
    }

    @Test
    public void resolve_strictModeSysProp_throwsExceptionOnMissingVar() {
        System.setProperty("casc.strict.secret.resolution", "true");
        try {
            IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
                resolve("${ANOTHER_MISSING_VAR}");
            });
            assertThat(exception.getMessage(), containsString("ANOTHER_MISSING_VAR"));
        } finally {
            System.clearProperty("casc.strict.secret.resolution");
        }
    }

    @Test
    public void resolve_strictMode_ignoresExceptionIfDefaultProvided() {
        environment.set("CASC_STRICT_SECRET_RESOLUTION", "true");

        String output = resolve("${MISSING_SECRET_VAR:-my_fallback_value}");

        assertThat(output, equalTo("my_fallback_value"));
    }

    @Test
    public void resolve_strictModeSetToFalse_defaultsToEmptyString() {
        environment.set("CASC_STRICT_SECRET_RESOLUTION", "false");

        String output = resolve("${MISSING_SECRET_VAR}");

        assertThat(output, equalTo(""));
        assertTrue(logContains("Configuration import: Found unresolved variable 'MISSING_SECRET_VAR'"));
    }

    @Test
    public void resolve_strictMode_multipleVariables_oneMissing_shouldFail() {
        environment.set("CASC_STRICT_SECRET_RESOLUTION", "true");
        environment.set("FOO", "hello");

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            resolve("${FOO}:${MISSING}");
        });
        assertThat(exception.getMessage(), containsString("MISSING"));
    }

    @Test
    public void resolve_decryptRsaLookup() throws Exception {
        try (RsaTestContext ctx = new RsaTestContext()) {
            String secret = "resolver-integration-secret";
            String ciphertext = ctx.encrypt(secret);
            String output = resolve("${decrypt:jwe:" + ciphertext + "}");

            assertThat(output, equalTo(secret));
        }
    }

    private static KeyPair createRsaKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static String encryptJwe(String secret, PublicKey publicKey, String kid) throws Exception {
        JWEHeader header =
                new JWEHeader.Builder(RSA_OAEP_256, A256GCM).keyID(kid).build();

        JWEObject jwe = new JWEObject(header, new Payload(secret));
        jwe.encrypt(new RSAEncrypter((RSAPublicKey) publicKey));

        return jwe.serialize();
    }

    private static void writeRsaPrivateKey(Path path, PrivateKey privateKey) throws IOException {
        String encoded = getMimeEncoder(64, new byte[] {'\n'}).encodeToString(privateKey.getEncoded());

        String pem = "-----BEGIN PRIVATE KEY-----\n" + encoded + "\n-----END PRIVATE KEY-----\n";

        Files.writeString(path, pem, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static final class RsaTestContext implements AutoCloseable {

        final KeyPair keyPair;
        final Path keyFile;
        final String originalKeyProperty;
        final String kid;

        RsaTestContext() throws Exception {
            originalKeyProperty = System.getProperty(KEY_PROP);
            keyPair = createRsaKeyPair();
            kid = computeKeyId((RSAPrivateCrtKey) keyPair.getPrivate());

            keyFile = Files.createTempFile("casc-interp-test", ".pem");
            writeRsaPrivateKey(keyFile, keyPair.getPrivate());
            System.setProperty(KEY_PROP, keyFile.toAbsolutePath().toString());
            clearRsaEngineCache();
        }

        String encrypt(String plaintext) throws Exception {
            return encryptJwe(plaintext, keyPair.getPublic(), kid);
        }

        private static String computeKeyId(RSAPrivateCrtKey key) throws Exception {
            RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(key.getModulus(), key.getPublicExponent());
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(publicKey.getEncoded());

            byte[] truncated = Arrays.copyOf(hash, 8);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(truncated);
        }

        @Override
        public void close() throws Exception {
            if (originalKeyProperty == null) {
                System.clearProperty(KEY_PROP);
            } else {
                System.setProperty(KEY_PROP, originalKeyProperty);
            }
            clearRsaEngineCache();
            Files.deleteIfExists(keyFile);
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
    }

    @Test
    public void resolve_decrypt_plaintextWithMissingVariable_isPreservedLiterally() throws Exception {
        try (RsaTestContext ctx = new RsaTestContext()) {
            String plaintext = "p${UNSET}ss";
            String ciphertext = ctx.encrypt(plaintext);

            String output = resolve("${decrypt:jwe:" + ciphertext + "}");

            assertThat("Decrypted value containing ${UNSET} must not be interpolated", output, equalTo(plaintext));
        }
    }

    @Test
    public void resolve_decrypt_plaintextThatIsOnlyAVariable_isPreservedLiterally() throws Exception {
        try (RsaTestContext ctx = new RsaTestContext()) {
            String plaintext = "${ENV_VAR}";
            String ciphertext = ctx.encrypt(plaintext);

            String output = resolve("${decrypt:jwe:" + ciphertext + "}");

            assertThat("Decrypted value '${ENV_VAR}' must not be further interpolated", output, equalTo(plaintext));
        }
    }

    @Test
    public void resolve_decrypt_plaintextWithSurroundingText_isPreservedLiterally() throws Exception {
        try (RsaTestContext ctx = new RsaTestContext()) {
            String plaintext = "prefix-${missing}-suffix";
            String ciphertext = ctx.encrypt(plaintext);

            String output = resolve("${decrypt:jwe:" + ciphertext + "}");

            assertThat("Decrypted value 'prefix-${missing}-suffix' must remain unchanged", output, equalTo(plaintext));
        }
    }

    @Test
    public void resolve_decrypt_ordinaryPlaintext_resolvesCorrectly() throws Exception {
        try (RsaTestContext ctx = new RsaTestContext()) {
            String plaintext = "normal-password";
            String ciphertext = ctx.encrypt(plaintext);

            String output = resolve("${decrypt:jwe:" + ciphertext + "}");

            assertThat("Ordinary plaintext must resolve to its decrypted value", output, equalTo(plaintext));
        }
    }

    @Test
    public void resolve_decrypt_strictMode_plaintextWithMissingVariable_isPreservedLiterally() throws Exception {
        environment.set("CASC_STRICT_SECRET_RESOLUTION", "true");

        try (RsaTestContext ctx = new RsaTestContext()) {
            String plaintext = "p${UNSET}ss";
            String ciphertext = ctx.encrypt(plaintext);

            String output = resolve("${decrypt:jwe:" + ciphertext + "}");

            assertThat(
                    "Strict mode: decrypted value with ${UNSET} must not be interpolated", output, equalTo(plaintext));
        }
    }

    @Test
    public void resolve_decrypt_strictMode_plainEnvVarOutside_stillThrows() {
        environment.set("CASC_STRICT_SECRET_RESOLUTION", "true");

        assertThrows(IllegalStateException.class, () -> resolve("${TRULY_MISSING_OUTSIDE_DECRYPT}"));
    }

    @Test
    public void resolve_decrypt_embeddedInLargerString_isPreservedLiterally() throws Exception {
        try (RsaTestContext ctx = new RsaTestContext()) {
            String plaintext = "sec${UNSET}ret";
            String ciphertext = ctx.encrypt(plaintext);

            String output = resolve("prefix-${decrypt:jwe:" + ciphertext + "}-suffix");

            assertThat(
                    "Decrypted value embedded in larger string must not be interpolated",
                    output,
                    equalTo("prefix-" + plaintext + "-suffix"));
        }
    }

    @Test
    public void resolve_nonDecryptVariables_behaviorUnchanged() {
        environment.set("MY_VAR", "hello");
        assertThat(resolve("${MY_VAR}"), equalTo("hello"));
        assertThat(resolve("${UNDEFINED_VAR}"), equalTo(""));
        assertThat(resolve("${UNDEFINED_VAR:-fallback}"), equalTo("fallback"));
    }

    @Test
    public void resolve_multipleDecryptExpressions_resolvesBothCorrectly() throws Exception {
        try (RsaTestContext ctx = new RsaTestContext()) {
            String secret1 = "first-password";
            String secret2 = "second-password";
            String cipher1 = ctx.encrypt(secret1);
            String cipher2 = ctx.encrypt(secret2);

            String output = resolve("User:${decrypt:jwe:" + cipher1 + "}:Pass:${decrypt:jwe:" + cipher2 + "}");

            assertThat(
                    "Multiple decrypt expressions in one input must all resolve correctly",
                    output,
                    equalTo("User:first-password:Pass:second-password"));
        }
    }

    @Test
    public void resolve_decrypt_plaintextWithNestedDecrypt_isPreservedLiterally() throws Exception {
        try (RsaTestContext ctx = new RsaTestContext()) {
            String plaintext = "prefix-${decrypt:jwe:fake-ciphertext}-suffix";
            String ciphertext = ctx.encrypt(plaintext);

            String output = resolve("${decrypt:jwe:" + ciphertext + "}");

            assertThat(
                    "Decrypted plaintext containing a nested decrypt expression must remain literal",
                    output,
                    equalTo(plaintext));
        }
    }

    @Test
    public void resolve_failedResolutionFollowedBySuccess_leavesNoStaleState() throws Exception {
        environment.set("CASC_STRICT_SECRET_RESOLUTION", "true");
        environment.set("VALID_VAR", "success-value");

        try (RsaTestContext ctx = new RsaTestContext()) {
            IllegalStateException exception =
                    assertThrows(IllegalStateException.class, () -> resolve("${MISSING_VAR}"));
            assertThat(exception.getMessage(), containsString("MISSING_VAR"));

            String standardOutput = resolve("${VALID_VAR}");
            assertThat(
                    "Subsequent standard lookup must succeed without stale state",
                    standardOutput,
                    equalTo("success-value"));

            String plaintext = "my-secret";
            String ciphertext = ctx.encrypt(plaintext);
            String decryptOutput = resolve("${decrypt:jwe:" + ciphertext + "}");

            assertThat("Subsequent decrypt lookup must succeed without stale state", decryptOutput, equalTo(plaintext));
        }
    }
}
