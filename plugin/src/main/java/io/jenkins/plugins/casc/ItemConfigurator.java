package io.jenkins.plugins.casc;

import hudson.ExtensionPoint;
import hudson.model.TopLevelItem;
import io.jenkins.plugins.casc.model.CNode;

public interface ItemConfigurator<T extends TopLevelItem> extends ExtensionPoint {

    String getName();

    Class<T> getTarget();

    T configure(String name, CNode config, ConfigurationContext context) throws ConfiguratorException;
}
