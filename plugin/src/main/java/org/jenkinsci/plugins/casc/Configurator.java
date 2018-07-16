package org.jenkinsci.plugins.casc;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.remoting.Which;
import jenkins.model.Jenkins;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.casc.impl.configurators.ConfigurableConfigurator;
import org.jenkinsci.plugins.casc.impl.configurators.DataBoundConfigurator;
import org.jenkinsci.plugins.casc.impl.configurators.DescriptorConfigurator;
import org.jenkinsci.plugins.casc.impl.configurators.EnumConfigurator;
import org.jenkinsci.plugins.casc.impl.configurators.ExtensionConfigurator;
import org.jenkinsci.plugins.casc.impl.configurators.HeteroDescribableConfigurator;
import org.jenkinsci.plugins.casc.impl.configurators.PrimitiveConfigurator;
import org.jenkinsci.plugins.casc.model.CNode;
import org.jvnet.tiger_types.Types;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.Beta;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.lang.Klass;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;


/**
 * Defines a mapping between a tree that represents user configuration and a Jenkins object produced from it.
 *
 * <p>
 * Different {@link Configurator}s define mapping for different Jenkins objects.
 *
 * <p>
 * This mapping includes not just performing the instantiation of Jenkins objects but also static description
 * of the mapping to enable schema/doc generation.
 *
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Restricted(Beta.class)
public abstract class Configurator<T> implements ExtensionPoint, ElementConfigurator<T> {

    private final static Logger logger = Logger.getLogger(Configurator.class.getName());

    @CheckForNull
    public static RootElementConfigurator lookupRootElement(String name) {
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
    @Nonnull
    public static Configurator lookupOrFail(Type type) throws ConfiguratorException {
        Configurator cfg = lookup(type);
        if (cfg == null) {
            throw new ConfiguratorException("Cannot find configurator for type " + type);
        }
        return cfg;
    }

    /**
     * Looks for a configurator for exact type.
     * @param type Type
     * @return Configurator or {@code null} if it is not found
     */
    @CheckForNull
    public static Configurator lookup(Type type) {
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

        if (getDataBoundConstructor(clazz) != null) {
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

    /**
     * Finds a Configurator for base type and a short name
     * @param clazz Base class
     * @param shortname Short name of the implementation
     * @return Configurator
     */
    @CheckForNull
    public static Configurator lookupForBaseType(Class<?> clazz, @Nonnull String shortname) {
        final Jenkins jenkins = Jenkins.getInstance();
        final ExtensionList<Configurator> l = jenkins.getExtensionList(Configurator.class);
        for (Configurator c : l) {
            if (shortname.equalsIgnoreCase(c.getName())) { // short name match, ensure that the type is compliant
                if (clazz.isAssignableFrom(c.getTarget())) { // Implements child class
                    return c;
                }
            }
        }
        return null;
    }

    @CheckForNull
    public static Constructor getDataBoundConstructor(@Nonnull Class type) {
        for (Constructor c : type.getConstructors()) {
            if (c.getAnnotation(DataBoundConstructor.class) != null) return c;
        }
        return null;

    }

    @Nonnull
    public static String normalize(@Nonnull String name) {
        if (name.toUpperCase().equals(name)) {
            name = name.toLowerCase();
        } else {
            name = StringUtils.uncapitalize(name);
        }
        return name;
    }


    // ---

    /**
     *
     * @return the list of Configurator to be considered so one can fully configure this component.
     * Typically, an extension point with multiple implementations will return Configurators for available implementations.
     */
    @Nonnull
    public List<Configurator> getConfigurators() {
        return Collections.singletonList(this);
    }

    @Override
    public String getName() {
        final Symbol annotation = getTarget().getAnnotation(Symbol.class);
        if (annotation != null) return annotation.value()[0];
        return normalize(getTarget().getSimpleName());
    }

    /**
     * {@inheritDoc}
     */
    public abstract Class<T> getTarget();

    public boolean match(Class clazz) {
        return clazz == getTarget();
    }

    /**
     * The extension point being implemented by this configurator.
     *
     * @return Extension point or {@code null} if undefined
     */
    @CheckForNull
    public Class getExtensionPoint() {
        Class t = getTarget();
        if (ExtensionPoint.class.isAssignableFrom(t)) return t;
        return t;
    }


    /**
     * Retrieve which plugin do provide this extension point
     *
     * @return String representation of the extension source, usually artifactId.
     *         {@code null} if {@link #getExtensionPoint()} returns {@code null}.
     * @throws IOException if config file cannot be saved
     */
    @CheckForNull
    public String getExtensionSource() throws IOException {
        final Class e = getExtensionPoint();
        if (e == null) return null;
        final String jar = Which.jarFile(e).getName();
        if (jar.startsWith("jenkins-core-")) return "jenkins-core"; // core jar has version in name
        return jar.substring(0, jar.lastIndexOf('.'));
    }


    //TODO: replace by a ClassName by default?
    /**
     * Human friendly display name for this component.
     *
     * @return Display name or empty string
     */
    @CheckForNull
    public String getDisplayName() { return ""; }

    private Klass getKlass() {
        return Klass.java(getTarget());
    }

    /**
     * {@inheritDoc}
     * @param config
     */
    @Nonnull
    public abstract T configure(CNode config) throws ConfiguratorException;

    /**
     * Describe a component as a Configuration Nodes {@link CNode} to be exported as yaml.
     * Only export attributes which are <b>not</b> set to default value.
    @CheckForNull
    public abstract CNode describe(T instance) throws Exception;
     */


    /**
     * Ordered version of {@link #describe()} for documentation generation.
     * Only include non-deprecated, non-restricted attribute
     *
     * @return
     *      A list of {@link Attribute}s
     */
    @Nonnull
    public List<Attribute> getAttributes() {
        return describe().stream()
                .filter(a -> !a.isRestricted())
                .filter(a -> !a.isDeprecated())
                .sorted(Comparator.comparing(a -> a.name))
                .collect(Collectors.toList());
    }

    @CheckForNull
    public <V> Attribute<T, V> getAttribute(@Nonnull String name) {
        Set<Attribute> attrs = describe();
        for (Attribute attr : attrs) {
            if (attr.name.equalsIgnoreCase(name)) {
                return attr;
            }
        }
        return null;
    }

    /**
     * Retrieve the html help tip associated to an attribute.
     * FIXME would prefer &lt;st:include page="help-${a.name}.html" class="${c.target}" optional="true"/&gt;
     * @param attribute to get help for
     * @return String that shows help. May be empty
     * @throws IOException if the resource cannot be read
     */
    @Nonnull
    public String getHtmlHelp(String attribute) throws IOException {
        final URL resource = getKlass().getResource("help-" + attribute + ".html");
        if (resource != null) {
            return IOUtils.toString(resource.openStream());
        }
        return "";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Configurator) {
            return getTarget() == ((Configurator) obj).getTarget();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getTarget().hashCode();
    }
}
