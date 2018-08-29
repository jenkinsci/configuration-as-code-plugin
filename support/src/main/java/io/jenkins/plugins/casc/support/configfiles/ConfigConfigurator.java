package io.jenkins.plugins.casc.support.configfiles;

import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.xml.XmlConfig;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import hudson.Extension;
import io.jenkins.plugins.casc.impl.configurators.HeteroDescribableConfigurator;

/**
 * Configurator for the different {@link Config} types, e.g. {@link XmlConfig}.
 *
 * @author srempfer
 *
 */
@Extension
@Restricted(NoExternalUse.class)
public class ConfigConfigurator extends HeteroDescribableConfigurator<Config> {

    public ConfigConfigurator() {
        super(Config.class);
    }
}
