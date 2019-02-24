package io.jenkins.plugins.casc;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.jvnet.hudson.test.JenkinsRule;

public class SecretSourceResolverTest {

    @ClassRule
    public static JenkinsRule j = new JenkinsRule();

    @Rule
    public final EnvironmentVariables environment = new EnvironmentVariables();

    private static ConfigurationContext context;

    @BeforeClass
    public static void setup() {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        context = new ConfigurationContext(registry);
    }

    @Test
    public void resolve_singleEntry() {
        environment.set("FOO", "hello");
        assertThat(SecretSourceResolver.resolve(context, "${FOO}"), equalTo("hello"));
    }

    @Test
    public void resolve_singleEntryWithoutDefaultValue() {
        assertThat(SecretSourceResolver.resolve(context, "${FOO}"), equalTo(""));
    }

    @Test
    public void resolve_singleEntryWithDefaultValue() {
        assertThat(SecretSourceResolver.resolve(context, "${FOO:-default}"), equalTo("default"));
    }

    @Test
    public void resolve_singleEntryWithDefaultValueAndWithEnvDefined() {
        environment.set("FOO", "hello");
        assertThat(SecretSourceResolver.resolve(context, "${FOO:-default}"), equalTo("hello"));
    }

    @Test
    public void resolve_singleEntryEscaped() {
        assertThat(SecretSourceResolver.resolve(context, "^${FOO}"), equalTo("${FOO}"));
    }

    @Test
    public void resolve_multipleEntries() {
        environment.set("FOO", "hello");
        environment.set("BAR", "world");
        assertThat(SecretSourceResolver.resolve(context, "${FOO}:${BAR}"), equalTo("hello:world"));
    }

    @Test
    public void resolve_multipleEntriesWithoutDefaultValue() {
        assertThat(SecretSourceResolver.resolve(context, "${FOO}:${BAR}"), equalTo(":"));
    }

    @Test
    public void resolve_multipleEntriesWithDefaultValue() {
        assertThat(SecretSourceResolver.resolve(context, "${FOO:-hello}:${BAR:-world}"), equalTo("hello:world"));
    }

    @Test
    public void resolve_multipleEntriesWithDefaultValueAndEnvDefined() {
        environment.set("FOO", "hello");
        environment.set("BAR", "world");
        assertThat(SecretSourceResolver.resolve(context, "${FOO:-default}:${BAR:-default}"), equalTo("hello:world"));
    }

    @Test
    public void resolve_multipleEntriesEscaped() {
        assertThat(SecretSourceResolver.resolve(context, "^${FOO}:^${BAR}"), equalTo("${FOO}:${BAR}"));
    }

    @Test
    public void resolve_nothing() {
        assertThat(SecretSourceResolver.resolve(context, "FOO"), equalTo("FOO"));
    }

    @Test
    public void resolve_nothingSpace() {
        assertThat(SecretSourceResolver.resolve(context, "${ }"), equalTo("${ }"));
    }

    @Test
    public void resolve_nothingBrackets() {
        assertThat(SecretSourceResolver.resolve(context, "${}"), equalTo("${}"));
    }

    @Test
    public void resolve_nothingDefault() {
        assertThat(SecretSourceResolver.resolve(context, "${:-default}"), equalTo("default"));
    }

    @Test
    public void resolve_emptyDefault() {
        assertThat(SecretSourceResolver.resolve(context, "${FOO:-}"), equalTo(""));
    }

    @Test
    public void resolve_emptyDefaultEnvDefined() {
        environment.set("FOO", "foo");
        assertThat(SecretSourceResolver.resolve(context, "${FOO:-}"), equalTo("foo"));
    }

    @Test
    public void resolve_defaultValueLimit() {
        assertThat(SecretSourceResolver.resolve(context, "${FOO:-default:-other}"), equalTo("default:-other"));
    }

    @Test
    public void resolve_mixedSingleEntry() {
        environment.set("FOO", "www.foo.io");
        assertThat(SecretSourceResolver.resolve(context, "http://${FOO}"), equalTo("http://www.foo.io"));
    }

    @Test
    public void resolve_mixedSingleEntryWithDefault() {
        environment.set("FOO", "www.foo.io");
        assertThat(SecretSourceResolver.resolve(context, "${protocol:-https}://${FOO:-www.bar.io}"), equalTo("https://www.foo.io"));
    }

    @Test
    public void resolve_mixedSingleEntryEscaped() {
        assertThat(SecretSourceResolver.resolve(context, "http://^${FOO}"), equalTo("http://${FOO}"));
    }

    @Test
    public void resolve_mixedMultipleEntries() {
        environment.set("FOO", "www.foo.io");
        environment.set("BAR", "8080");
        assertThat(SecretSourceResolver.resolve(context, "http://${FOO}:${BAR}"), equalTo("http://www.foo.io:8080"));
    }

    @Test
    public void resolve_mixedMultipleEntriesWithDefault() {
        environment.set("FOO", "www.foo.io");
        environment.set("protocol", "http");
        assertThat(SecretSourceResolver.resolve(context, "${protocol:-https}://${FOO:-www.bar.io}"), equalTo("http://www.foo.io"));
    }

    @Test
    public void resolve_mixedMultipleEntriesEscaped() {
        assertThat(SecretSourceResolver.resolve(context, "http://^${FOO}:^${BAR}"), equalTo("http://${FOO}:${BAR}"));
    }
}
