package io.jenkins.plugins.casc.yaml;

import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.AttributeTest;
import io.jenkins.plugins.casc.impl.configurators.DataBoundConfigurator;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Contains tests for particular export cases.
 */
public class YamlExportTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    @Issue("SECURITY-1458")
    public void shouldDiscoverSecretsBasedOnTheAttributeType() {
        DataBoundConfigurator c = new DataBoundConfigurator<>(AttributeTest.SecretRenamedFieldFithSecretConstructor.class);
        Set<Attribute> attributes = c.describe();
        assertThat(attributes.size(), equalTo(1));
        Attribute attr = attributes.iterator().next();
        assertTrue(attr.isSecret(null));
    }
}
