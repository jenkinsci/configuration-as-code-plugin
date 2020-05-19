package io.jenkins.plugins.casc;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.ManagementLink;
import io.jenkins.plugins.casc.impl.configurators.DescriptorConfigurator;
import io.jenkins.plugins.casc.impl.configurators.GlobalConfigurationCategoryConfigurator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import jenkins.model.GlobalConfigurationCategory;
import jenkins.model.Jenkins;

/**
 * Define a {@link Configurator} which handles a root configuration element, identified by name.
 * Note: we assume any configurator here will use a unique name for root element.
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */

public interface RootElementConfigurator<T> extends Configurator<T> {

    static List<RootElementConfigurator> all() {
        final Jenkins jenkins = Jenkins.get();
        List<RootElementConfigurator> configurators = new ArrayList<>(
            jenkins.getExtensionList(RootElementConfigurator.class));

        for (GlobalConfigurationCategory category : GlobalConfigurationCategory.all()) {
            configurators.add(new GlobalConfigurationCategoryConfigurator(category));
        }

        for (ManagementLink link : ManagementLink.all()) {
            final String name = link.getUrlName();
            final Descriptor descriptor = Jenkins.get().getDescriptor(name);
            if (descriptor != null)
                configurators.add(new DescriptorConfigurator(descriptor));
        }

        configurators.sort(Comparator.comparingDouble(c -> {
            Extension extension = c.getClass().getAnnotation(Extension.class);
            if (extension == null) {
                return Double.MIN_VALUE;
            }
            return extension.ordinal();
        }).reversed());

        return configurators;
    }

    /* This function is used for configurator-pointer in the documentation.jelly file only. */
    default boolean isRootElement() {
        return true;
    }

    /**
     * Retrieve the target component managed by this RootElementConfigurator
     * @return
     * @param context
     */
    T getTargetComponent(ConfigurationContext context);
}
