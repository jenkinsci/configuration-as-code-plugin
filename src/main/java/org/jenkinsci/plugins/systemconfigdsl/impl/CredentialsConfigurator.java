package org.jenkinsci.plugins.systemconfigdsl.impl;

import com.google.auto.service.AutoService;
import org.jenkinsci.plugins.systemconfigdsl.api.Configurator;

@AutoService(Configurator.class)
public class CredentialsConfigurator extends Configurator {

    @Override
    public String getConfigFileSectionName() {
        return "credentials";
    }

    @Override
    public void configure(Object config) {
        System.out.println("Configuring credentials: " + config.toString());
    }
}
