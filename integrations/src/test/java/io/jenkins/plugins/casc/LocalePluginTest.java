package io.jenkins.plugins.casc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import hudson.plugins.locale.PluginImpl;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.Rule;
import org.junit.Test;

public class LocalePluginTest {
    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("locale.yaml")
    public void should_configure_locale_plugin() {
        PluginImpl localePlugin = PluginImpl.get();
        assertEquals("en", localePlugin.getSystemLocale());
        assertTrue("Expected ignoreAcceptLanguage to be true", localePlugin.isIgnoreAcceptLanguage());
    }
}
