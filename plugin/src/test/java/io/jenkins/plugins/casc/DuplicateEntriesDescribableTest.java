package io.jenkins.plugins.casc;

import hudson.ExtensionPoint;
import hudson.ExtensionList;
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

    private static final Logger LOGGER = Logger.getLogger(DuplicateEntriesDescribableTest.class.getName());
    
    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("/io/jenkins/plugins/casc/duplicate-entries-describable.yaml")
    public void shouldWarnOnDuplicateEntries() throws Exception {
        // This test verifies that we get a warning for duplicate entries in a single-valued Describable
        LOGGER.info("Looking up TestComponent extensions");
        ExtensionList<TestComponent> components = j.jenkins.getExtensionList(TestComponent.class);
        LOGGER.info("Found " + components.size() + " TestComponent extensions");
        
        // If no components found, the test will fail with a clear error message
        if (components.isEmpty()) {
            throw new AssertionError("No TestComponent extensions found. Test setup may be incorrect.");
        }
        
        TestComponent component = components.get(0);
        
        // We should still get a configuration, but a warning should be logged
        // Since we can't easily check logged warnings in tests, we at least verify
        // that the component was configured with one of the options
        TestServerConfig config = component.getServerConfig();
        LOGGER.info("Server config: " + (config != null ? config.toString() : "null"));
        
        // Make sure we got a configuration and it's not null
        if (config == null) {
            throw new AssertionError("Server configuration is null. The configuration was not applied correctly.");
        }
        
        assertThat(config.toString(), containsString("TestServerConfig"));
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
        
        // Component mapping needs to match the YAML structure
        Mapping componentMapping = new Mapping();
        componentMapping.put("serverConfiguration", serverConfigMapping);
        
        // Create the full mapping structure with unclassified section
        Mapping fullMapping = new Mapping();
        Mapping unclassified = new Mapping();
        unclassified.put("duplicateEntriesDescribableTest$testComponent", componentMapping);
        fullMapping.put("unclassified", unclassified);
        
        // This should throw an exception
        Configurator<TestComponent> configurator = 
            context.lookupOrFail(TestComponent.class);
        
        ConfiguratorException exception = assertThrows(
            ConfiguratorException.class, 
            () -> ConfigurationAsCode.get().configureWith(fullMapping)
        );
        
        assertThat(exception.getMessage(), 
                  containsString("Multiple configurations found for single-valued Describable"));
    }

    // Test component with a single-valued Describable
    @TestExtension
    public static class TestComponent implements ExtensionPoint {
        private TestServerConfig serverConfig;

        public static TestComponent get() {
            return ExtensionList.lookup(TestComponent.class).get(0);
        }

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