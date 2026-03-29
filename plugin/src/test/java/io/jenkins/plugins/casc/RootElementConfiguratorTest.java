package io.jenkins.plugins.casc;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import hudson.model.ManagementLink;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;

public class RootElementConfiguratorTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    @SuppressWarnings("rawtypes")
    public void shouldSurviveBrokenManagementLink() {
        List<RootElementConfigurator> configurators = RootElementConfigurator.all();

        assertNotNull("Configurators list should not be null", configurators);
        assertFalse("Should load at least some standard configurators", configurators.isEmpty());
    }

    @TestExtension("shouldSurviveBrokenManagementLink")
    @SuppressWarnings("unused")
    public static class BrokenManagementLink extends ManagementLink {

        @Override
        public String getIconFileName() {
            return null;
        }

        @Override
        public String getDisplayName() {
            return "Broken Cluster Stats Link";
        }

        @Override
        public String getUrlName() {
            throw new IllegalStateException("cannot call getRootUrlFromRequest from outside a request handling thread");
        }
    }

    @TestExtension("shouldSurviveBrokenManagementLink")
    @SuppressWarnings("unused")
    public static class EmptyStringManagementLink extends ManagementLink {
        @Override
        public String getIconFileName() {
            return null;
        }

        @Override
        public String getDisplayName() {
            return "Empty Link";
        }

        @Override
        public String getUrlName() {
            return "";
        }
    }
}
