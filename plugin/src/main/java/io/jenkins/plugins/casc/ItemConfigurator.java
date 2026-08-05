package io.jenkins.plugins.casc;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.TopLevelItem;
import io.jenkins.plugins.casc.model.CNode;

public interface ItemConfigurator<T extends TopLevelItem> extends Configurator<T> {

    @NonNull
    String getName();

    Class<T> getTarget();

    T configure(String name, CNode config, ConfigurationContext context) throws ConfiguratorException;
}
