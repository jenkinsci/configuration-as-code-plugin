package org.jenkinsci.plugins.systemconfigdsl;

import jenkins.model.Jenkins;
import org.jenkinsci.plugins.systemconfigdsl.api.Configurator;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.logging.Logger;

public class ServiceImplementationsLoader {
    private final Map<String, Configurator> configurators = new HashMap();
    private static final Logger LOGGER = Logger.getLogger(ConfigurationLoader.class.getName());

    public ServiceImplementationsLoader() {
        LOGGER.info("Loading configurator implementations");
        loadConfigurators();
    }

    private void loadConfigurators() {
        final ServiceLoader<Configurator> loader = ServiceLoader.load(Configurator.class,
                Jenkins.getInstance().getPluginManager().uberClassLoader);
        final Iterator<Configurator> configuratorIterator = loader.iterator();
        while (configuratorIterator.hasNext()) {
            Configurator configurator = configuratorIterator.next();
            this.configurators.put(configurator.getConfigFileSectionName(), configurator);
        }
        LOGGER.info("Registered the following configurator implementations: " + this.configurators.keySet().toString());
    }

    public void reloadConfigurators() {
        LOGGER.info("Reloading configurator implementations");
        loadConfigurators();
    }

    public Map<String, Configurator> getConfigurators() {
        return configurators;
    }
}


