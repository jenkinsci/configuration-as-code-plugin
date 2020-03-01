package io.jenkins.plugins.casc;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import io.jenkins.plugins.casc.impl.configurators.DataBoundConfigurator;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.snakeyaml.error.YAMLException;
import io.jenkins.plugins.casc.snakeyaml.nodes.Node;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertTrue;

public class Security1446Test {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    private static final String PATH_PATTERN = "path = \\$\\{PATH\\}";
    private static final String JAVA_HOME_PATTERN = "java-home = \\$\\{JAVA_HOME\\}";

    @ConfiguredWithCode("Security1446Test.yml")
    @Test
    @Issue("SECURITY-1446")
    public void testImportWithEnvVar() {
        List<StandardUsernamePasswordCredentials> userPasswCred = CredentialsProvider.lookupCredentials(StandardUsernamePasswordCredentials.class,Jenkins.getInstanceOrNull(), null, Collections.emptyList());
        assertThat(userPasswCred.size(), is(1));
        for (StandardUsernamePasswordCredentials cred : userPasswCred) {
            assertTrue("The JAVA_HOME environment variable should not be resolved", cred.getUsername().matches(JAVA_HOME_PATTERN));
            assertTrue("The PATH environment variable should not be resolved", cred.getDescription().matches(PATH_PATTERN));
        }

        List<StringCredentials> stringCred = CredentialsProvider.lookupCredentials(StringCredentials.class,Jenkins.getInstanceOrNull(), null, Collections.emptyList());
        assertThat(stringCred.size(), is(1));
        for (StringCredentials cred : stringCred) {
            assertTrue("The PATH environment variable should not be resolved", cred.getDescription().matches(PATH_PATTERN));
        }
    }

    @Test
    @Issue("SECURITY-1446")
    public void testExportWithEnvVar() throws Exception {
        final String message = "Hello, world! PATH=${PATH} JAVA_HOME=^${JAVA_HOME}";
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);

        DataBoundConfigurator<UsernamePasswordCredentialsImpl> configurator = new DataBoundConfigurator<>(UsernamePasswordCredentialsImpl.class);
        UsernamePasswordCredentialsImpl creds = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, "test",
                message, "foo", "bar");
        final CNode config = configurator.describe(creds, context);
        final Node valueNode = ConfigurationAsCode.get().toYaml(config);
        final String exported;
        try (StringWriter writer = new StringWriter()) {
            ConfigurationAsCode.serializeYamlNode(valueNode, writer);
            exported = writer.toString();
        } catch (IOException e) {
            throw new YAMLException(e);
        }

        assertThat("Message was not escaped", exported, not(containsString(message)));
        assertThat("Improper masking for PATH", exported, containsString("^${PATH}"));
        assertThat("Improper masking for JAVA_HOME", exported, containsString("^^${JAVA_HOME}"));
    }

}
