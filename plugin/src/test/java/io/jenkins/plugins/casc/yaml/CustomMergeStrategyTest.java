package io.jenkins.plugins.casc.yaml;

import static io.jenkins.plugins.casc.ConfigurationContext.CASC_MERGE_STRATEGY_ENV;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThrows;

import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.jvnet.hudson.test.JenkinsSessionRule;
import org.jvnet.hudson.test.TestExtension;
import org.yaml.snakeyaml.nodes.Node;

public class CustomMergeStrategyTest {

    @Rule
    public JenkinsSessionRule session = new JenkinsSessionRule();

    @Rule
    public final EnvironmentVariables environment = new EnvironmentVariables();

    @Test
    public void customMergeStrategy_withoutEnvironment() throws Throwable {
        session.then(r -> {
            assertThat(System.getenv(CASC_MERGE_STRATEGY_ENV), nullValue());
            customMergeStrategy();
        });
    }

    @Test
    public void customMergeStrategy_withEnvironment() throws Throwable {
        environment.set(CASC_MERGE_STRATEGY_ENV, "override");
        session.then(r -> {
            assertThat(System.getenv(CASC_MERGE_STRATEGY_ENV), is("override"));
            customMergeStrategy();
        });
    }

    private void customMergeStrategy() {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext customContext = new ConfigurationContext(registry, "custom-test");
        String normalSource = getClass().getResource("normal.yml").toExternalForm();
        String overwriteSource = getClass().getResource("overwrite.yml").toExternalForm();

        List<YamlSource> list = List.of(YamlSource.of(normalSource), YamlSource.of(overwriteSource));

        ConfiguratorException exception =
                assertThrows(ConfiguratorException.class, () -> YamlUtils.merge(list, customContext));
        assertThat(exception.getMessage(), is("custom-test merge strategy"));
    }

    @TestExtension
    public static class CustomTestMergeStrategy implements MergeStrategy {
        @Override
        public void merge(Node firstNode, Node secondNode, String source) throws ConfiguratorException {
            throw new ConfiguratorException("custom-test merge strategy");
        }

        @Override
        public String getName() {
            return "custom-test";
        }
    }
}
