package io.jenkins.plugins.casc;

import io.jenkins.plugins.casc.SecretSourceResolver.FileStringLookup;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

public class SecretSourceResolverTest {

    @ClassRule
    public static JenkinsRule j = new JenkinsRule();

    private static ConfigurationContext context;

    @Rule
    public final EnvironmentVariables environment = new EnvironmentVariables();

    @Rule
    public LoggerRule logging = new LoggerRule();
    public static final StringLookup ENCODE = StringLookupFactory.INSTANCE.base64EncoderStringLookup();
    public static final StringLookup DECODE = StringLookupFactory.INSTANCE.base64DecoderStringLookup();
    public static final StringLookup FILE = FileStringLookup.INSTANCE;

    @Before
    public void initLogging() {
        logging.record(Logger.getLogger(SecretSourceResolver.class.getName()), Level.INFO).capture(2048);
    }

    @BeforeClass
    public static void setUp() {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        context = new ConfigurationContext(registry);
    }

    @Test
    public void resolve_singleEntry() {
        environment.set("FOO", "hello");
        assertThat(resolve("${FOO}"), equalTo("hello"));
    }

    public String resolve(String toInterpolate) {
        return context.getSecretSourceResolver().resolve(toInterpolate);
    }

    public boolean logContains(String text) {
        final String expectedText = text;
        return logging.getMessages().stream().anyMatch(m -> m.contains(expectedText));
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

    @Test
    public void resolve_mixedMultipleEntriesWithDefault() {
        environment.set("FOO", "www.foo.io");
        environment.set("protocol", "http");
        assertThat(resolve("${protocol:-https}://${FOO:-www.bar.io}"), equalTo("http://www.foo.io"));
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
        String input = Paths.get(getClass().getResource("secret.json").toURI()).toFile().getAbsolutePath();
        String output = resolve("${file:" + input + "}");
        assertThat(output, equalTo(FILE.lookup(input)));
        assertThat(output, containsString("\"Our secret\": \"Hello World\""));
    }

    @Test
    public void resolve_FileWithRelative() throws Exception {
        Path path = Paths.get(getClass().getResource("secret.json").toURI());
        String input = Paths.get("").toUri().relativize(path.toUri()).getPath();
        String output = resolve("${file:" + input + "}");
        assertThat(output, equalTo(FILE.lookup(input)));
        assertThat(output, containsString("\"Our secret\": \"Hello World\""));
    }

    @Test
    public void resolve_FileWithSpace() throws Exception {
        String path = Paths.get(getClass().getResource("some secret.json").toURI()).toFile().getAbsolutePath();
        String output = resolve("${file:" + path + "}");
        assertThat(output, equalTo(FILE.lookup(path)));
        assertThat(output, containsString("\"Our secret\": \"Hello World\""));
    }

    @Test
    public void resolve_FileWithSpaceAndRelative() throws Exception {
        String path = Paths.get(getClass().getResource("some secret.json").toURI()).toFile().getAbsolutePath();
        String input = Paths.get("").toUri().relativize(new File(path).toURI()).getPath();
        String output = resolve("${file:" + input + "}");
        assertThat(output, equalTo(FILE.lookup(input)));
        assertThat(output, containsString("\"Our secret\": \"Hello World\""));
    }

    @Test
    public void resolve_FileBase64() throws Exception {
        String input = Paths.get(getClass().getResource("secret.json").toURI()).toFile().getAbsolutePath();
        String output = resolve("${base64:${file:" + input + "}}");
        String decoded = DECODE.lookup(output);
        String content = FILE.lookup(input);
        assertThat(output, equalTo(ENCODE.lookup(content)));
        assertThat(decoded, equalTo(content));
        assertThat(decoded, containsString("\"Our secret\": \"Hello World\""));
    }

    @Test
    public void resolve_FileBase64NestedEnv() throws Exception {
        String input = Paths.get(getClass().getResource("secret.json").toURI()).toFile().getAbsolutePath();
        environment.set("FOO", input);
        String output = resolve("${base64:${file:${FOO}}}");
        String decoded = DECODE.lookup(output);
        String content = FILE.lookup(input);
        assertThat(output, equalTo(ENCODE.lookup(content)));
        assertThat(decoded, equalTo(content));
        assertThat(decoded, containsString("\"Our secret\": \"Hello World\""));
    }

    @Test
    public void resolve_FileNotFound() {
        resolve("${file:./hello-world-not-found.txt}");
        assertTrue(logContains("Configuration import: Error looking up file './hello-world-not-found.txt' with UTF-8 encoding."));
        assertTrue(logContains("Configuration import: Found unresolved variable 'file:./hello-world-not-found.txt'."));
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
}
