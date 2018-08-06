package io.jenkins.plugins.casc;

import hudson.ExtensionPoint;
import hudson.remoting.Which;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import io.jenkins.plugins.casc.model.CNode;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.Beta;
import org.kohsuke.stapler.lang.Klass;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URL;
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

    /**
     *
     * @return the list of Configurator to be considered so one can fully configure this component.
     * Typically, an extension point with multiple implementations will return Configurators for available implementations.
     * @param context
     */
    @Nonnull
    public List<Configurator> getConfigurators(ConfigurationContext context) {
        return Collections.singletonList(this);
    }

    @Override
    public String getName() {
        final Symbol annotation = getTarget().getAnnotation(Symbol.class);
        if (annotation != null) return annotation.value()[0];
        return normalize(getTarget().getSimpleName());
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
     * @param context
     */
    @Nonnull
    public abstract T configure(CNode config, ConfigurationContext context) throws ConfiguratorException;

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
