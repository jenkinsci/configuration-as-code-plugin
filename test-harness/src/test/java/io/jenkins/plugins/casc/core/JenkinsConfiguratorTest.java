package io.jenkins.plugins.casc.core;

import hudson.EnvVars;
import hudson.model.TaskListener;
import hudson.model.labels.LabelAtom;
import hudson.model.labels.LabelAtomProperty;
import hudson.model.labels.LabelAtomPropertyDescriptor;
import hudson.security.FullControlOnceLoggedInAuthorizationStrategy;
import hudson.security.HudsonPrivateSecurityRealm;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.util.DescribableList;
import io.jenkins.plugins.casc.ConfigurationAsCode;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import java.io.ByteArrayOutputStream;
import java.io.File;
import jenkins.model.Jenkins;
import org.apache.commons.io.FileUtils;
import org.jenkinsci.Symbol;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.TestExtension;
import org.kohsuke.stapler.DataBoundConstructor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class JenkinsConfiguratorTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("Primitives.yml")
    public void jenkins_primitive_attributes() throws Exception {
        final Jenkins jenkins = Jenkins.get();
        assertEquals(6666, jenkins.getSlaveAgentPort());
    }

    @Test
    @ConfiguredWithCode("HeteroDescribable.yml")
    public void jenkins_abstract_describable_attributes() throws Exception {
        final Jenkins jenkins = Jenkins.get();
        assertTrue(jenkins.getSecurityRealm() instanceof HudsonPrivateSecurityRealm);
        assertTrue(jenkins.getAuthorizationStrategy() instanceof FullControlOnceLoggedInAuthorizationStrategy);
        assertFalse(((FullControlOnceLoggedInAuthorizationStrategy) jenkins.getAuthorizationStrategy()).isAllowAnonymousRead());
    }

    @Test
    @Issue("Issue #173")
    @ConfiguredWithCode("SetEnvironmentVariable.yml")
    public void shouldSetEnvironmentVariable() throws Exception {
        final DescribableList<NodeProperty<?>, NodePropertyDescriptor> properties = Jenkins.get().getNodeProperties();
        EnvVars env = new EnvVars();
        for (NodeProperty<?> property : properties) {
            property.buildEnvVars(env, TaskListener.NULL);
        }
        assertEquals("BAR", env.get("FOO"));
    }

    @Test
    @ConfiguredWithCode("ConfigureLabels.yml")
    public void shouldExportLabelAtoms() throws Exception {
        DescribableList properties = Jenkins.get().getLabelAtom("label1").getProperties();
        properties.add(new TestProperty(1));

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        ConfigurationAsCode.get().export(out);
        final String exported = out.toString();

        String content = FileUtils.readFileToString(new File(getClass().getResource("ExpectedLabelsConfiguration.yml").toURI()), "UTF-8");
        assertThat(exported, containsString(content));
    }

    @Test
    @ConfiguredWithCode("ConfigureLabels.yml")
    public void shouldImportLabelAtoms() {
        LabelAtom label1 = Jenkins.get().getLabelAtom("label1");
        assertNotNull(label1);
        assertThat(label1.getProperties(), hasSize(2));
        assertEquals(2, label1.getProperties().get(TestProperty.class).value);
        assertEquals(4, label1.getProperties().get(AnotherTestProperty.class).otherProperty);

        LabelAtom label2 = Jenkins.get().getLabelAtom("label2");
        assertNotNull(label2);
        assertThat(label2.getProperties(), hasSize(1));
        assertEquals(3, label2.getProperties().get(TestProperty.class).value);
    }

    public static class TestProperty extends LabelAtomProperty {

        public final int value;

        @DataBoundConstructor
        public TestProperty(int value) {
            this.value = value;
        }

        @TestExtension
        @Symbol("myProperty")
        public static class DescriptorImpl extends LabelAtomPropertyDescriptor {
            @Override
            public String getDisplayName() {
                return "A simple value";
            }
        }
    }

    public static class AnotherTestProperty extends LabelAtomProperty {

        public final int otherProperty;

        @DataBoundConstructor
        public AnotherTestProperty(int otherProperty) {
            this.otherProperty = otherProperty;
        }

        @TestExtension
        public static class DescriptorImpl extends LabelAtomPropertyDescriptor {
            @Override
            public String getDisplayName() {
                return "Another simple value";
            }
        }
    }

}
