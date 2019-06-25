/*
 * Copyright (c) 2018 CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to
 * whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package io.jenkins.plugins.casc;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.ExtensionPoint;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;

/**
 * Define a {@link Configurator} which handles a configuration element, identified by name.
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 * @author Oleg Nenashev
 * @see RootElementConfigurator
 */

public interface Configurator<T> {

    @NonNull
    static String normalize(@NonNull String name) {
        if (name.toUpperCase().equals(name)) {
            name = name.toLowerCase();
        } else {
            name = StringUtils.uncapitalize(name);
        }
        return name;
    }

    /**
     * Get a configurator name.
     * @return short name for this component when used in a configuration.yaml file
     */
    @NonNull
    default String getName() {
        final Symbol annotation = getTarget().getAnnotation(Symbol.class);
        if (annotation != null) return annotation.value()[0];
        return normalize(getTarget().getSimpleName());
    }

    /**
     * @return Human friendly display name for this component, used in generated documentation.
     */
    default String getDisplayName() { return getName(); }

    /**
     * Target type this configurator can handle.
     */
    Class<T> getTarget();

    /**
     * @return <code>true</code> if this configurator can handle type <code>clazz</code>. Implementation has to be
     * consistent with {@link #getTarget()}
     */
    default boolean canConfigure(Class clazz) {
        return clazz == getTarget();
    }

    /**
     *
     *
     * @return The API implemented by target type, i.e. implemented {@link ExtensionPoint} for components to implement
     * some jenkins APIs, or raw type for others.
     */
    @NonNull
    default Class getImplementedAPI() {
        return getTarget();
    }


    /**
     * @return list of {@link Configurator<T>}s to be considered so one can fully configure this component.
     * Typically, configurator for an abstract extension point will return Configurators for available implementations.
     */
    @NonNull
    default List<Configurator<T>> getConfigurators(ConfigurationContext context) {
        return Collections.singletonList(this);
    }


    /**
     * Determine the list of Attribute available for configuration of the managed component.
     *
     * @return A set of {@link Attribute}s that describes this object
     */
    @NonNull
    Set<Attribute<T,?>> describe();


    /**
     * @return Ordered version of {@link #describe()} for documentation generation.
     * Only include non-ignored attribute
     */
    @NonNull
    default List<Attribute<T,?>> getAttributes() {
        return describe().stream()
                .filter(a -> !a.isIgnored())
                .sorted(Comparator.comparing(a -> a.name))
                .collect(Collectors.toList());
    }

    /**
     * Configures/creates a Jenkins object based on a tree.
     *
     * @param config
     *      Map/List/primitive objects (think YAML) that represents the configuration from which
     *      a Jenkins object is configured.
     * @param context
     * @return
     *      Fully configured Jenkins object that results from this configuration.
     *      if no new objects got created, but some existing objects may have been modified, return updated target object.
     * @throws ConfiguratorException if something went wrong, depends on the concrete implementation
     */
    @NonNull
    T configure(CNode config, ConfigurationContext context) throws ConfiguratorException;

    /**
     * Run the same logic as {@link #configure(CNode, ConfigurationContext)} in dry-run mode.
     * Used to verify configuration is fine before being actually applied to a live jenkins master.
     * @param config
     * @param context
     * @throws ConfiguratorException
     */
    T check(CNode config, ConfigurationContext context) throws ConfiguratorException;


    /**
     * Describe a component as a Configuration Nodes {@link CNode} to be exported as yaml.
     * Only export attributes which are <b>not</b> set to default value.
     */
    @CheckForNull
    default CNode describe(T instance, ConfigurationContext context) throws Exception {
        Mapping mapping = new Mapping();
        for (Attribute attribute : getAttributes()) {
            CNode value = attribute.describe(instance, context);
            if (value != null) {
                mapping.put(attribute.getName(), value);
            }
        }
        return mapping;
    }

}
