package org.jenkinsci.plugins.systemconfigdsl.api;

public abstract class Configurator {

    public abstract String getConfigFileSectionName();

    public abstract void configure(Object config);
}