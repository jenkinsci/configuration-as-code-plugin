package org.jenkinsci.plugins.casc;

import hudson.ExtensionList;
import hudson.model.Descriptor;
import jenkins.model.GlobalConfigurationCategory;
import jenkins.model.Jenkins;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Define a {@link Configurator} which handles a root configuration element, identified by name.
 * Note: we assume any configurator here will use a unique name for root element.
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public interface RootElementConfigurator<T> extends ElementConfigurator<T> {

    static List<RootElementConfigurator> all() {
        List<RootElementConfigurator> configurators = new ArrayList<>();
        final Jenkins jenkins = Jenkins.getInstance();
        configurators.addAll(jenkins.getExtensionList(RootElementConfigurator.class));

        for (GlobalConfigurationCategory category : GlobalConfigurationCategory.all()) {
            configurators.add(new GlobalConfigurationCategoryConfigurator(category));
        }

        return configurators;
    }
}
