package io.jenkins.plugins.casc;

import jenkins.model.Jenkins;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.Beta;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.lang.reflect.Type;

/**
 * A Registry to allow {@link Configurator}s retrieval.
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Restricted(Beta.class)
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
    Configurator lookup(Type type);

    /**
     * null-safe flavour of {@link #lookup(Type)}.
     * @param type
     * @throws ConfiguratorException if we don't know any {@link RootElementConfigurator} for requested type
     */
    @Nonnull
    Configurator lookupOrFail(Type type) throws ConfiguratorException;

    /**
     * Retrieve default implementation from Jenkins
     */
    static ConfiguratorRegistry get() {
        return Jenkins.getInstance().getExtensionList(ConfiguratorRegistry.class).get(0);
    }
}
