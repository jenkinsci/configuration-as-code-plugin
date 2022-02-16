package io.jenkins.plugins.casc.yaml;

import io.jenkins.plugins.casc.CasCGlobalConfig;
import io.jenkins.plugins.casc.ConfigurationAsCode;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import java.util.Set;
import jenkins.model.Jenkins;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.ExpectedException;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class OverrideMergeStrategyTest {

    @ClassRule
    public static JenkinsRule j = new JenkinsRule();

    private static ConfigurationContext context;

    @ClassRule
    public static final EnvironmentVariables environment = new EnvironmentVariables();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @BeforeClass
    public static void setUp() {
        environment.set("CASC_MERGE_STRATEGY", "override");
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        context = new ConfigurationContext(registry);
    }

    @Test
    public void merge() throws ConfiguratorException {
        String normalSource = getClass().getResource("normal.yml").toExternalForm();
        String overwriteSource = getClass().getResource("overwrite.yml").toExternalForm();
        String conflictsSource = getClass().getResource("conflicts.yml").toExternalForm();

        // merge without conflicts
        ConfigurationAsCode.get().configure(normalSource, overwriteSource);

        // merge with conflicts
        ConfigurationAsCode.get().configure(normalSource, conflictsSource);
    }

    @Test
    public void incompatible() throws ConfiguratorException {
        String normalSource = getClass().getResource("normal.yml").toExternalForm();
        String incompatibleSource = getClass().getResource("incompatible.yml").toExternalForm();

        assertThrows("Incompatible config files merging process", ConfiguratorException.class,
            () -> ConfigurationAsCode.get().configure(normalSource, incompatibleSource));
    }

    @Test
    public void hasCorrectDefaultName() {
        MergeStrategy strategy = MergeStrategyFactory.getMergeStrategyOrDefault("override");
        assertNotNull(strategy);
        assertEquals("override", strategy.getName());
    }

    @Test
    public void simpleStringValue() throws ConfiguratorException {
        String sysA = getClass().getResource("systemMessage-a.yml").toExternalForm();
        String sysB = getClass().getResource("systemMessage-b.yml").toExternalForm();

        // merge without conflicts
        ConfigurationAsCode.get().configure(sysA, sysB);

        assertEquals("unexpected systemMessage with override merge strategy",
            "hello b", Jenkins.get().getSystemMessage());
    }

    @Test
    public void sequenceValue() throws ConfiguratorException {
        String sequenceA = getClass().getResource("sequence-a.yml").toExternalForm();
        String sequenceB = getClass().getResource("sequence-b.yml").toExternalForm();

        // merge without conflicts
        ConfigurationAsCode.get().configure(sequenceA, sequenceB);

        Set<String> agentProtocals = Jenkins.get().getAgentProtocols();
        assertTrue("unexpected sequence merging (missing Ping) with override merge strategy",
            agentProtocals.contains("Ping"));
        assertTrue("unexpected sequence merging (missing JNLP4-connect) with override merge strategy",
            agentProtocals.contains("JNLP4-connect"));
    }

    @Test
    public void multipleKeys() throws ConfiguratorException {
        String multipleKeysA = getClass().getResource("multiple-keys-a.yml").toExternalForm();
        String multipleKeysB = getClass().getResource("multiple-keys-b.yml").toExternalForm();

        CasCGlobalConfig descriptor = (CasCGlobalConfig) j.jenkins.getDescriptor(CasCGlobalConfig.class);
        assertNotNull(descriptor);

        // merge without conflicts, A <- B
        ConfigurationAsCode.get().configure(multipleKeysA, multipleKeysB);
        assertEquals("b", descriptor.getConfigurationPath());

        // merge without conflicts, B <- A
        ConfigurationAsCode.get().configure(multipleKeysB, multipleKeysA);
        assertEquals("a", descriptor.getConfigurationPath());
    }
}
