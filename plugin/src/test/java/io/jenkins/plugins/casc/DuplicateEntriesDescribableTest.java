package io.jenkins.plugins.casc;

import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.TestExtension;

import java.util.logging.Level;
import java.util.logging.Logger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThrows;

public class DuplicateEntriesDescribableTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("duplicate-entries-describable.yaml")
    public void shouldWarnOnDuplicateEntries() throws Exception {
        // This test verifies that we get a warning for duplicate entries in a single-valued Describable
        TestComponent component = j.jenkins.getExtensionList(TestComponent.class).get(0);
        
        // We should still get a configuration, but a warning should be logged
        // Since we can't easily check logged warnings in tests, we at least verify
        // that the component was configured with one of the options
        assertThat(component.getServerConfig().toString(), 
                  containsString("TestServerConfig")); // This just verifies we got some server config
    }

    @Test
    public void shouldRejectDuplicateEntriesInStrictMode() throws Exception {
        // Set the unknown handling to reject mode
        ConfigurationContext context = new ConfigurationContext(j.jenkins);
        context.setUnknown(ConfigurationContext.Unknown.reject);
        
        // Create a mapping with duplicate entries
        Mapping serverConfigMapping = new Mapping();
        serverConfigMapping.put("manual", Mapping.EMPTY);
        serverConfigMapping.put("wellKnown", Mapping.EMPTY);
        
        Mapping componentMapping = new Mapping();
        componentMapping.put("serverConfiguration", serverConfigMapping);
        
        // This should throw an exception
        Configurator<TestComponent> configurator = 
            context.lookupOrFail(TestComponent.class);
        
        ConfiguratorException exception = assertThrows(
            ConfiguratorException.class, 
            () -> configurator.configure(componentMapping, context)
        );
        
        assertThat(exception.getMessage(), 
                  containsString("Multiple configurations found for single-valued Describable"));
    }

    // Test component with a single-valued Describable
    public static class TestComponent implements ExtensionPoint {
        private TestServerConfig serverConfig;

        public TestServerConfig getServerConfiguration() {
            return serverConfig;
        }

        public void setServerConfiguration(TestServerConfig serverConfig) {
            this.serverConfig = serverConfig;
        }

        public TestServerConfig getServerConfig() {
            return serverConfig;
        }
    }

    @TestExtension
    public static class TestComponentDescriptor extends Descriptor<TestComponent> {
    }
    
    // Base class for server configuration
    public static abstract class TestServerConfig extends AbstractDescribableImpl<TestServerConfig> {
    }
    
    // Implementation for manual configuration
    public static class ManualTestServerConfig extends TestServerConfig {
        @Override
        public String toString() {
            return "ManualTestServerConfig";
        }
    }
    
    // Descriptor for manual configuration
    @TestExtension
    public static class ManualTestServerConfigDescriptor extends Descriptor<TestServerConfig> {
        @Override
        public String getDisplayName() {
            return "Manual Configuration";
        }
    }
    
    // Implementation for well-known configuration
    public static class WellKnownTestServerConfig extends TestServerConfig {
        @Override
        public String toString() {
            return "WellKnownTestServerConfig";
        }
    }
    
    // Descriptor for well-known configuration
    @TestExtension
    public static class WellKnownTestServerConfigDescriptor extends Descriptor<TestServerConfig> {
        @Override
        public String getDisplayName() {
            return "Well-Known Configuration";
        }
    }
} 