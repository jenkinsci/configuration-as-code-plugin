package io.jenkins.plugins.casc;

import hudson.ExtensionList;
import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import org.codefirst.SimpleThemeDecorator;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * @author v1v (Victor Martinez)
 */
public class SimpleThemeTest {

    @Rule
    public JenkinsConfiguredWithReadmeRule j = new JenkinsConfiguredWithReadmeRule();

    @Test
    @ConfiguredWithReadme("simple-theme-plugin/README.md")
    public void configure_simple_theme() {
        // Already tested within the plugin itself, let's run some basic tests.
        // https://github.com/jenkinsci/simple-theme-plugin/blob/master/src/test/java/org/jenkinsci/plugins/simpletheme/ConfigurationAsCodeTest.java
        SimpleThemeDecorator decorator = ExtensionList.lookupSingleton(SimpleThemeDecorator.class);
        assertNotNull(decorator);
    }
}
