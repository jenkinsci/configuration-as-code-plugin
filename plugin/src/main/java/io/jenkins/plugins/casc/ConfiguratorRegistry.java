package io.jenkins.plugins.casc;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.lang.reflect.Type;
import jenkins.model.Jenkins;

/**
 * A Registry to allow {@link Configurator}s retrieval.
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */

public interface ConfiguratorRegistry {

    /**
     * Retrieve a {@link RootElementConfigurator} by it's yaml element (key) name.
     * @param name
     * @return <code>null</code> if we don't know any {@link RootElementConfigurator} for requested name
     */
    @CheckForNull
    RootElementConfigurator lookupRootElement(String name);

    /**
     * Retrieve a {@link Configurator} for target type.
     * @param type
     * @return <code>null</code> if we don't know any {@link RootElementConfigurator} for requested type
     */
    @CheckForNull
    <T> Configurator<T> lookup(Type type);

    /**
     * null-safe flavour of {@link #lookup(Type)}.
     * @param type
     * @throws ConfiguratorException if we don't know any {@link RootElementConfigurator} for requested type
     */
    @NonNull
    <T> Configurator<T> lookupOrFail(Type type) throws ConfiguratorException;

    /**
     * Retrieve default implementation from Jenkins
     */
    static ConfiguratorRegistry get() {
        return Jenkins.get().getExtensionList(ConfiguratorRegistry.class).get(0);
    }
}
