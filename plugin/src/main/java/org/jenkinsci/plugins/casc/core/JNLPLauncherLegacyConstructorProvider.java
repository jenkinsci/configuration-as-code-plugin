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

package org.jenkinsci.plugins.casc.core;

import hudson.Extension;
import hudson.slaves.JNLPLauncher;
import org.jenkinsci.plugins.casc.LegacyDataBoundConstructorProvider;
import org.jenkinsci.plugins.casc.impl.configurators.DataBoundConfigurator;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import javax.annotation.Nonnull;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.Set;

/**
 * Provides information about Legacy constructors for {@link JNLPLauncher}.
 * @see DataBoundConfigurator
 * @author Oleg Nenashev
 * @since TODO
 */
@Extension(optional = true)
@Restricted(NoExternalUse.class)
public class JNLPLauncherLegacyConstructorProvider
        extends LegacyDataBoundConstructorProvider<JNLPLauncher> {

    @Nonnull
    @Override
    public Set<Constructor<JNLPLauncher>> getConstructorsFor(@Nonnull Class<?> clazz) {
        if (!clazz.equals(JNLPLauncher.class)) {
            return Collections.emptySet();
        }

        try {
            return Collections.singleton(JNLPLauncher.class.getConstructor(
                    String.class, String.class));
        } catch (NoSuchMethodException e) {
            // Deprecated Method has been removed or so, we do not care here
            return Collections.emptySet();
        }
    }
}
