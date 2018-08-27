package io.jenkins.plugins.casc;

import hudson.model.Descriptor;
import hudson.model.ManagementLink;
import io.jenkins.plugins.casc.impl.configurators.DescriptorConfigurator;
import io.jenkins.plugins.casc.impl.configurators.GlobalConfigurationCategoryConfigurator;
import jenkins.model.GlobalConfigurationCategory;
import jenkins.model.Jenkins;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.Beta;

import java.util.ArrayList;
import java.util.List;

/**
 * Define a {@link Configurator} which handles a root configuration element, identified by name.
 * Note: we assume any configurator here will use a unique name for root element.
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Restricted(Beta.class)
public interface RootElementConfigurator<T> extends Configurator<T> {

    static List<RootElementConfigurator> all() {
        List<RootElementConfigurator> configurators = new ArrayList<>();
        final Jenkins jenkins = Jenkins.getInstance();
        configurators.addAll(jenkins.getExtensionList(RootElementConfigurator.class));

        for (GlobalConfigurationCategory category : GlobalConfigurationCategory.all()) {
            configurators.add(new GlobalConfigurationCategoryConfigurator(category));
        } 

        for (ManagementLink link : ManagementLink.all()) {
            final String name = link.getUrlName();
            final Descriptor descriptor = Jenkins.getInstance().getDescriptor(name);
            if (descriptor != null)
                configurators.add(new DescriptorConfigurator(descriptor));
        }

        return configurators;
    }

    /**
     * Retrieve the target component managed by this RootElementConfigurator
     * @return
     * @param context
     */
    T getTargetComponent(ConfigurationContext context);
}
