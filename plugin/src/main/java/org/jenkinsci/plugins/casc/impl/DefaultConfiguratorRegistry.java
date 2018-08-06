package org.jenkinsci.plugins.casc.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.casc.Configurable;
import org.jenkinsci.plugins.casc.Configurator;
import org.jenkinsci.plugins.casc.ConfiguratorException;
import org.jenkinsci.plugins.casc.ConfiguratorRegistry;
import org.jenkinsci.plugins.casc.RootElementConfigurator;
import org.jenkinsci.plugins.casc.impl.configurators.ConfigurableConfigurator;
import org.jenkinsci.plugins.casc.impl.configurators.DataBoundConfigurator;
import org.jenkinsci.plugins.casc.impl.configurators.DescriptorConfigurator;
import org.jenkinsci.plugins.casc.impl.configurators.EnumConfigurator;
import org.jenkinsci.plugins.casc.impl.configurators.ExtensionConfigurator;
import org.jenkinsci.plugins.casc.impl.configurators.HeteroDescribableConfigurator;
import org.jenkinsci.plugins.casc.impl.configurators.PrimitiveConfigurator;
import org.jvnet.tiger_types.Types;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.Stapler;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension
@Restricted(NoExternalUse.class)
public class DefaultConfiguratorRegistry implements ConfiguratorRegistry {

    private final static Logger logger = Logger.getLogger(DefaultConfiguratorRegistry.class.getName());


    @Override
    @CheckForNull
    public RootElementConfigurator lookupRootElement(String name) {
        for (RootElementConfigurator c : RootElementConfigurator.all()) {
            if (c.getName().equalsIgnoreCase(name)) {
                return c;
            }
        }
        return null;
    }

    /**
     * Looks for a configurator for exact type.
     * @param type Type
     * @return Configurator
     * @throws ConfiguratorException Configurator is not found
     */
    @Override
    @Nonnull
    public Configurator lookupOrFail(Type type) throws ConfiguratorException {
        try {
            return cache.get(type);
        } catch (ExecutionException e) {
            throw (ConfiguratorException) e.getCause();
        }
    }

    /**
     * Looks for a configurator for exact type.
     * @param type Type
     * @return Configurator or {@code null} if it is not found
     */
    @Override
    @CheckForNull
    public Configurator lookup(Type type) {
        try {
            return cache.get(type);
        } catch (ExecutionException e) {
            return null;
        }
    }

    private LoadingCache<Type, Configurator> cache = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.SECONDS)
            .build(new CacheLoader<Type, Configurator>() {
                @Override
                public Configurator load(Type type) throws Exception {
                    final Configurator configurator = internalLookup(type);
                    if (configurator == null) throw new ConfiguratorException("Cannot find configurator for type " + type);
                    return configurator;
                }
            });

    private Configurator internalLookup(Type type) {
        Class clazz = Types.erasure(type);

        final Jenkins jenkins = Jenkins.getInstance();
        final ExtensionList<Configurator> l = jenkins.getExtensionList(Configurator.class);
        for (Configurator c : l) {
            if (c.match(clazz)) {
                // this type has a dedicated Configurator implementation
                return c;
            }
        }

        if (Collection.class.isAssignableFrom(clazz)) {
            //TODO: Only try to cast if we can actually get the parameterized type
            if (type instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) type;
                Type actualType = pt.getActualTypeArguments()[0];
                if (actualType instanceof WildcardType) {
                    actualType = ((WildcardType) actualType).getUpperBounds()[0];
                }
                if(actualType instanceof ParameterizedType){
                    actualType = ((ParameterizedType)actualType).getRawType();
                }
                if (!(actualType instanceof Class)) {
                    throw new IllegalStateException("Can't handle " + type);
                }
                return lookup(actualType);
            }
        }

        if (Configurable.class.isAssignableFrom(clazz)) {
            return new ConfigurableConfigurator(clazz);
        }

        if (Descriptor.class.isAssignableFrom(clazz)) {
            return new DescriptorConfigurator((Descriptor) jenkins.getExtensionList(clazz).get(0));
        }

        if (DataBoundConfigurator.getDataBoundConstructor(clazz) != null) {
            return new DataBoundConfigurator(clazz);
        }

        if (Modifier.isAbstract(clazz.getModifiers()) && Describable.class.isAssignableFrom(clazz)) {
            // this is a jenkins Describable component, with various implementations
            return new HeteroDescribableConfigurator(clazz);
        }

        if (Extension.class.isAssignableFrom(clazz)) {
            return new ExtensionConfigurator(clazz);
        }

        if (Stapler.lookupConverter(clazz) != null) {
            return new PrimitiveConfigurator(clazz);
        }

        if (clazz.isEnum()) {
            return new EnumConfigurator(clazz);
        }

        logger.warning("Configuration-as-Code can't handle type "+ type);
        return null;
    }


}
