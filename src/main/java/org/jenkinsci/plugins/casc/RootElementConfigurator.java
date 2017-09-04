package org.jenkinsci.plugins.casc;

import java.util.Set;

/**
 * Define a {@link Configurator} which handles a root configuration element, identified by name.
 * Note: we assume any configurator here will use a unique name for root element.
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public interface RootElementConfigurator {

    String getName();

    Set<Attribute> describe();
}
