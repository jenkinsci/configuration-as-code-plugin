package io.jenkins.plugins.casc.yaml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.jenkins.plugins.casc.ConfigurationAsCode;
import io.jenkins.plugins.casc.ConfiguratorException;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class ErrorOnConflictMergeStrategyTest {

    @Test
    void merge(JenkinsRule j) throws ConfiguratorException {
        String normalSource = getClass().getResource("normal.yml").toExternalForm();
        String overwriteSource = getClass().getResource("overwrite.yml").toExternalForm();
        String conflictsSource = getClass().getResource("conflicts.yml").toExternalForm();

        // merge without conflicts
        ConfigurationAsCode.get().configure(normalSource, overwriteSource);

        // merge with conflicts
        assertThrows(
                ConfiguratorException.class,
                () -> ConfigurationAsCode.get().configure(normalSource, conflictsSource),
                "Merging two conflict config files");
    }

    @Test
    void incompatible(JenkinsRule j) throws ConfiguratorException {
        String normalSource = getClass().getResource("normal.yml").toExternalForm();
        String incompatibleSource = getClass().getResource("incompatible.yml").toExternalForm();

        assertThrows(
                ConfiguratorException.class,
                () -> ConfigurationAsCode.get().configure(normalSource, incompatibleSource),
                "Incompatible config files merging process");
    }

    @Test
    void hasCorrectDefaultName(JenkinsRule j) {
        MergeStrategy strategy = MergeStrategyFactory.getMergeStrategyOrDefault(null);
        assertNotNull(strategy);
        assertEquals(MergeStrategy.DEFAULT_STRATEGY, strategy.getName());
    }
}
