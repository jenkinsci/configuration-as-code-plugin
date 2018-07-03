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

package org.jenkinsci.plugins.casc;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.Beta;

import javax.annotation.Nonnull;
import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Set;

/**
 * Provides resolution logic for Legacy {@link org.kohsuke.stapler.DataBoundConstructor}s.
 * This extension point exists to support explicit specifications of compatible constructors if
 * the annotated one does not work.
 *
 * This extension point can be used as a workaround for some cases when there
 * is a need to modify {@link org.kohsuke.stapler.DataBoundConstructor} in
 * a way not compatible with JCasC (or to support existing modifications).
 * It <b>should not</b> be used to defined for defining custom initialization logic,
 * new {@link Configurator}s should be created instead.
 *
 * @see DataBoundConfigurator
 * @author Oleg Nenashev
 * @since TODO
 */
@Restricted(Beta.class)
public abstract class LegacyDataBoundConstructorProvider<TTarget> implements ExtensionPoint {

    @Nonnull
    public abstract Set<Constructor<TTarget>> getConstructorsFor(@Nonnull Class<?> clazz);

    /**
     * Locates legacy {@link org.kohsuke.stapler.DataBoundConstructor}s
     * @param targetClazz Target class which should be produced by constructors
     * @param <T> Target class which should be produced by constructors
     * @return List of Legacy constructors defined by extension points.
     *         The list is ordered depending on Extension point ordinals.
     */
    @Nonnull
    public static <T> Set<Constructor<T>> getLegacyDataBoundConstructors(@Nonnull Class<T> targetClazz) {
        HashSet<Constructor<T>> constructors = new HashSet<>();
        for (LegacyDataBoundConstructorProvider<?> provider : all()) {
            final Set<? extends Constructor<?>> provided = provider.getConstructorsFor(targetClazz);
            for (Constructor<?> pr : provided) {
                if (targetClazz.isAssignableFrom(pr.getDeclaringClass())) {
                    constructors.add((Constructor<T>) pr);
                } else {
                    throw new IllegalStateException("Extension " + provider + " provided a wrong constructor type for " + targetClazz);
                }
            }
        }
        return constructors;
    }

    @Nonnull
    public static ExtensionList<LegacyDataBoundConstructorProvider> all() {
        return ExtensionList.lookup(LegacyDataBoundConstructorProvider.class);
    }
}
