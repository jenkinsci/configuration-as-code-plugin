package io.jenkins.plugins.casc.yaml;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.AttributeTest;
import io.jenkins.plugins.casc.impl.configurators.DataBoundConfigurator;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Contains tests for particular export cases.
 */
@WithJenkins
class YamlExportTest {

    @Test
    @Issue("SECURITY-1458")
    void shouldDiscoverSecretsBasedOnTheAttributeType(JenkinsRule j) {
        DataBoundConfigurator c =
                new DataBoundConfigurator<>(AttributeTest.SecretRenamedFieldFithSecretConstructor.class);
        Set<Attribute> attributes = c.describe();
        assertThat(attributes.size(), equalTo(1));
        Attribute attr = attributes.iterator().next();
        assertTrue(attr.isSecret(null));
    }
}
