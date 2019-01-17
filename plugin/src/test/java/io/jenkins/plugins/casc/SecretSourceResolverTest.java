package io.jenkins.plugins.casc;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.jvnet.hudson.test.JenkinsRule;

public class SecretSourceResolverTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Rule
    public final EnvironmentVariables environment = new EnvironmentVariables();

    @Test
    public void resolve_singleEntry() {
        environment.set("FOO", "hello");
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        assertThat(SecretSourceResolver.resolve(context, "${FOO}"), equalTo("hello"));
    }

    @Test
    public void resolve_singleEntryWithoutDefaultValue() {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        assertThat(SecretSourceResolver.resolve(context, "${FOO}"), equalTo(""));
    }

    @Test
    public void resolve_singleEntryWithDefaultValue() {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        assertThat(SecretSourceResolver.resolve(context, "${FOO:-default}"), equalTo("default"));
    }

    @Test
    public void resolve_singleEntryEscaped() {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        assertThat(SecretSourceResolver.resolve(context, "^${FOO}"), equalTo("${FOO}"));
    }

    @Test
    public void resolve_multipleEntries() {
        environment.set("FOO", "hello");
        environment.set("BAR", "world");
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        assertThat(SecretSourceResolver.resolve(context, "${FOO}:${BAR}"), equalTo("hello:world"));
    }

    @Test
    public void resolve_multipleEntriesWithoutDefaultValue() {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        assertThat(SecretSourceResolver.resolve(context, "${FOO}:${BAR}"), equalTo(":"));
    }

    @Test
    public void resolve_multipleEntriesWithDefaultValue() {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        assertThat(SecretSourceResolver.resolve(context, "${FOO:-hello}:${BAR:-world}"), equalTo("hello:world"));
    }

    @Test
    public void resolve_multipleEntriesEscaped() {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        assertThat(SecretSourceResolver.resolve(context, "^${FOO}:^${BAR}"), equalTo("${FOO}:${BAR}"));
    }

    @Test
    public void resolve_nothing() {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        assertThat(SecretSourceResolver.resolve(context, "FOO"), equalTo("FOO"));
    }

    @Test
    public void resolve_defaultValueLimit() {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        assertThat(SecretSourceResolver.resolve(context, "${FOO:-default:-other}"), equalTo("default:-other"));
    }

    @Test
    public void resolve_mixedSingleEntry() {
        environment.set("FOO", "www.foo.io");
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        assertThat(SecretSourceResolver.resolve(context, "http://${FOO}"), equalTo("http://www.foo.io"));
    }

    @Test
    public void resolve_mixedSingleEntryEscaped() {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        assertThat(SecretSourceResolver.resolve(context, "http://^${FOO}"), equalTo("http://${FOO}"));
    }

    @Test
    public void resolve_mixedMultipleEntries() {
        environment.set("FOO", "www.foo.io");
        environment.set("BAR", "8080");
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        assertThat(SecretSourceResolver.resolve(context, "http://${FOO}:${BAR}"), equalTo("http://www.foo.io:8080"));
    }

    @Test
    public void resolve_mixedMultipleEntriesEscaped() {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        assertThat(SecretSourceResolver.resolve(context, "http://^${FOO}:^${BAR}"), equalTo("http://${FOO}:${BAR}"));
    }
}
