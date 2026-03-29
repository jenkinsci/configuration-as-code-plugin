package io.jenkins.plugins.casc;

import hudson.model.Descriptor;
import hudson.model.ManagementLink;
import io.jenkins.plugins.casc.impl.configurators.DescriptorConfigurator;
import io.jenkins.plugins.casc.impl.configurators.GlobalConfigurationCategoryConfigurator;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.GlobalConfigurationCategory;
import jenkins.model.Jenkins;

/**
 * Define a {@link Configurator} which handles a root configuration element, identified by name.
 * Note: we assume any configurator here will use a unique name for root element.
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public interface RootElementConfigurator<T> extends Configurator<T> {

    Logger LOGGER = Logger.getLogger(RootElementConfigurator.class.getName());

    static List<RootElementConfigurator> all() {
        final Jenkins jenkins = Jenkins.get();
        List<RootElementConfigurator> configurators =
                new ArrayList<>(jenkins.getExtensionList(RootElementConfigurator.class));

        for (GlobalConfigurationCategory category : GlobalConfigurationCategory.all()) {
            configurators.add(new GlobalConfigurationCategoryConfigurator(category));
        }

        for (ManagementLink link : ManagementLink.all()) {
            try {
                final String name = link.getUrlName();
                if (name != null && !name.isEmpty()) {
                    final Descriptor descriptor = Jenkins.get().getDescriptor(name);
                    if (descriptor != null) {
                        configurators.add(new DescriptorConfigurator(descriptor));
                    }
                }
            } catch (Exception | LinkageError e) {
                LOGGER.log(
                        Level.WARNING,
                        "Failed to load configuration for ManagementLink: "
                                + link.getClass().getName() + ". Skipping.",
                        e);
            }
        }

        configurators.sort(Configurator.extensionOrdinalSort());

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
