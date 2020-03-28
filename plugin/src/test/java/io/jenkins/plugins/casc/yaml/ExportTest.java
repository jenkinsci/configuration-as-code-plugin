package io.jenkins.plugins.casc.yaml;


import hudson.util.Secret;
import io.jenkins.plugins.casc.ConfigurationAsCode;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.impl.configurators.DataBoundConfigurator;
import io.jenkins.plugins.casc.model.CNode;
import java.io.IOException;
import java.io.StringWriter;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.DataBoundConstructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.Node;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Contains tests for particular export cases.
 */
public class ExportTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void shouldNotExportValuesWithSecretGetters() throws Exception {
        DataBoundConfigurator<DataBoundSecret> c = new DataBoundConfigurator<>(DataBoundSecret.class);
        String res = export(c, new DataBoundSecret("test"));
        assertThat(res, not(containsString("test")));
    }

    @Test
    @Issue("SECURITY-1458")
    public void shouldNotExportValuesWithSecretFields() throws Exception {
        DataBoundConfigurator<DataBoundSecretField> c = new DataBoundConfigurator<>(DataBoundSecretField.class);
        String res = export(c, new DataBoundSecretField("test"));
        assertThat(res, not(containsString("test")));
    }

    @Test
    @Issue("SECURITY-1458")
    public void shouldNotExportValuesWithSecretConstructors() throws Exception {
        DataBoundConfigurator<DataBoundSecretConstructor> c = new DataBoundConfigurator<>(DataBoundSecretConstructor.class);
        String res = export(c, new DataBoundSecretConstructor(Secret.fromString("test")));
        assertThat(res, not(containsString("test")));
    }

    public <T> String export(DataBoundConfigurator<T> configurator, T object) throws Exception {
        ConfigurationAsCode casc = ConfigurationAsCode.get();
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);

        final CNode config = configurator.describe(object, context);
        final Node valueNode = casc.toYaml(config);

        try (StringWriter writer = new StringWriter()) {
            ConfigurationAsCode.serializeYamlNode(valueNode, writer);
            return writer.toString();
        } catch (IOException e) {
            throw new YAMLException(e);
        }
    }

    public static class DataBoundSecret {

        Secret mySecretValue;

        @DataBoundConstructor
        public DataBoundSecret(String mySecretValue) {
            this.mySecretValue = Secret.fromString(mySecretValue);
        }

        public Secret getMySecretValue() {
            return mySecretValue;
        }
    }

    public static class DataBoundSecretField {

        Secret mySecretValue;

        @DataBoundConstructor
        public DataBoundSecretField(String mySecretValue) {
            this.mySecretValue = Secret.fromString(mySecretValue);
        }

        public String getMySecretValue() {
            return mySecretValue.getPlainText();
        }
    }

    /**
     * Example of a safe persistency to the disk when JCasC cannot discover the field.
     */
    public static class DataBoundSecretConstructor {

        Secret mySecretValueField;

        @DataBoundConstructor
        public DataBoundSecretConstructor(Secret mySecretValue) {
            this.mySecretValueField = mySecretValue;
        }

        public String getMySecretValue() {
            return mySecretValueField.getPlainText();
        }
    }

}
