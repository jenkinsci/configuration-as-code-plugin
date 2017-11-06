package org.jenkinsci.plugins.casc;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.remoting.Which;
import jenkins.model.Jenkins;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.lang.Klass;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public abstract class Configurator<T> implements ExtensionPoint {

    public static Configurator lookupRootElement(String name) {
        for (RootElementConfigurator c : ConfigurationAsCode.getRootConfigurators()) {
            if (c.getName().equals(name)) {
                return (Configurator) c;
            }
        }
        return null;
    }

    public static Configurator lookup(Type type) {

        Class clazz = asClass(type);

        final ExtensionList<Configurator> l = Jenkins.getInstance().getExtensionList(Configurator.class);
        for (Configurator c : l) {
            if (clazz == c.getTarget()) {
                // this type has a dedicated Configurator implementation
                return c;
            }
        }

        if (Collection.class.isAssignableFrom(clazz)) {
            ParameterizedType pt = (ParameterizedType) type;
            Type actualType = pt.getActualTypeArguments()[0];
            if (actualType instanceof WildcardType) {
                actualType = ((WildcardType) actualType).getUpperBounds()[0];
            }
            if (!(actualType instanceof Class)) {
                throw new IllegalStateException("Can't handle "+type);
            }
            return lookup(actualType);
        }

        if (Describable.class.isAssignableFrom(clazz)) {
            if (Modifier.isAbstract(clazz.getModifiers())) {
                return new HeteroDescribableConfigurator(clazz);
            }
            return new DescribableConfigurator(clazz);
        }

        if (Extension.class.isAssignableFrom(clazz)) {
            return new ExtensionConfigurator(clazz);
        }

        return new PrimitiveConfigurator(clazz);
    }

    private static Class asClass(Type type) {
        Class clazz;
        if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            clazz = (Class) pt.getRawType();
        } else {
            clazz = (Class) type;
        }
        return clazz;
    }

    // ---

    /**
     *
     * @return the list of Configurator to be considered so one can fully configure this component.
     * Typically, an extension point with multiple implementations will return Configurators for available implementations.
     */
    public List<Configurator> getConfigurators() {
        return Collections.singletonList(this);
    }

    /**
     * @return short name for this component when used in a configuration.yaml file
     */
    public String getName() {
        final Symbol annotation = getTarget().getAnnotation(Symbol.class);
        if (annotation != null) return annotation.value()[0];
        return getTarget().getSimpleName();
    }

    /**
     * The actual component being managed by this Configurator
     */
    public abstract Class<T> getTarget();

    /**
     * The extension point being implemented by this configurator. Can be null
     */
    public Class getExtensionPoint() {
        return ExtensionPoint.class.isAssignableFrom(getTarget()) ? getTarget() : null;
    }


    /**
     * Retrieve which plugin do provide this extension point
     */
    public String getExtensionSource() throws IOException {
        final Class e = getExtensionPoint();
        if (e == null) return null;
        final String jar = Which.jarFile(e).getName();
        if (jar.startsWith("jenkins-core-")) return "jenkins-core"; // core jar has version in name
        return jar.substring(0, jar.lastIndexOf('.'));
    }


    /**
     * Human friendly display name for this component.
     */
    public String getDisplayName() { return ""; }

    private Klass getKlass() {
        return Klass.java(getTarget());
    }

    public abstract T configure(Object config) throws Exception;

    /**
     * Ordered version of {@link #describe()} for documentation generation
     */
    public List<Attribute> getAttributes() {
        final ArrayList<Attribute> attributes = new ArrayList<>(describe());
        Collections.sort(attributes, (a,b) -> a.name.compareTo(b.name));
        return attributes;
    }

    /**
     * Determine the list of Attribute available for configuration of the managed component.
     */
    public abstract Set<Attribute> describe();

    /**
     * Retrieve the html help tip associated to an attribute.
     * FIXME would prefer <st:include page="help-${a.name}.html" class="${c.target}" optional="true"/>
     */
    public String getHtmlHelp(String attribute) throws IOException {
        final URL resource = getKlass().getResource("help-" + attribute + ".html");
        if (resource != null) {
            return IOUtils.toString(resource);
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
