package org.jenkinsci.plugins.casc;

import hudson.plugins.locale.PluginImpl;
import org.jenkinsci.plugins.casc.misc.ConfiguredWithCode;
import org.jenkinsci.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class LocaleTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode(value = "LocaleTest.yml")
    public void configure_locale() {
        PluginImpl locale = PluginImpl.get();
        Assert.assertEquals("FR_fr", locale.getSystemLocale());
        Assert.assertTrue(locale.isIgnoreAcceptLanguage());

    }
}
