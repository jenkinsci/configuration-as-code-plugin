package io.jenkins.plugins.casc.yaml;

import io.jenkins.plugins.casc.ConfigurationAsCode;
import io.jenkins.plugins.casc.ConfiguratorException;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

public class ErrorOnConflictMergeStrategyTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void merge() throws ConfiguratorException {
        String normalSource = getClass().getResource("normal.yml").toExternalForm();
        String overwriteSource = getClass().getResource("overwrite.yml").toExternalForm();
        String conflictsSource = getClass().getResource("conflicts.yml").toExternalForm();

        // merge without conflicts
        ConfigurationAsCode.get().configure(normalSource, overwriteSource);

        // merge with conflicts
        assertThrows("Merging two conflict config files", ConfiguratorException.class,
            () -> ConfigurationAsCode.get().configure(normalSource, conflictsSource));
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
        MergeStrategy strategy = MergeStrategyFactory.getMergeStrategyOrDefault(null);
        assertNotNull(strategy);
        assertEquals(MergeStrategy.DEFAULT_STRATEGY, strategy.getName());
    }
}
