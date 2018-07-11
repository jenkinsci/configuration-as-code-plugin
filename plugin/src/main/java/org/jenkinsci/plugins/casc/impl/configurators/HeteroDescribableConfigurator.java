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

package org.jenkinsci.plugins.casc.impl.configurators;

import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.security.HudsonPrivateSecurityRealm;
import hudson.security.SecurityRealm;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.casc.Attribute;
import org.jenkinsci.plugins.casc.Configurator;
import org.jenkinsci.plugins.casc.ConfiguratorException;
import org.jenkinsci.plugins.casc.ObsoleteConfigurationMonitor;
import org.jenkinsci.plugins.casc.impl.attributes.DescribableAttribute;
import org.jenkinsci.plugins.casc.model.CNode;
import org.jenkinsci.plugins.casc.model.Mapping;
import org.jenkinsci.plugins.casc.model.Scalar;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.Beta;

import javax.annotation.CheckForNull;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * {@link Configurator} that works with {@link Describable} subtype as a {@link #target}.
 *
 * <p>
 * The configuration object will be specify the 'short name' which we use to resolve to a specific
 * subtype of {@link #target}. For example, if {@link #target} is {@link SecurityRealm} and the short name
 * is 'local', we resolve to {@link HudsonPrivateSecurityRealm} (because it has {@link Symbol} annotation that
 * specifies that name.
 *
 * <p>
 * The corresponding {@link Configurator} will be then invoked to configure the chosen subtype.
 *
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Restricted(Beta.class)
public class HeteroDescribableConfigurator extends Configurator<Describable> {

    private static final Logger LOGGER = Logger.getLogger(HeteroDescribableConfigurator.class.getName());


    private final Class<Describable> target;

    public HeteroDescribableConfigurator(Class<Describable> clazz) {
        this.target = clazz;
    }

    @Override
    public Class<Describable> getTarget() {
        return target;
    }

    public List<Configurator> getConfigurators() {
        final List<Descriptor> candidates = Jenkins.getInstance().getDescriptorList(target);
        return candidates.stream()
                .map(d -> Configurator.lookup(d.getKlass().toJavaClass()))
                .filter(c -> c != null)
                .collect(Collectors.toList());
    }

    @Override
    public Describable configure(CNode config) throws ConfiguratorException {
        String shortname;
        CNode subconfig = null;
        switch (config.getType()) {
            case SCALAR:
                shortname = config.asScalar().getValue();
                break;
            case MAPPING:
                Mapping map = config.asMapping();
                if (map.size() != 1) {
                    throw new IllegalArgumentException("single entry map expected to configure a "+target.getName());
                }
                final Map.Entry<String, CNode> next = map.entrySet().iterator().next();
                shortname = next.getKey();
                subconfig = next.getValue();
                break;
            default:
                throw new IllegalArgumentException("Unexpected configuration type "+config);
        }

        final List<Descriptor> candidates = Jenkins.getInstance().getDescriptorList(target);

        Class<? extends Describable> k = findDescribableBySymbol(config, shortname, candidates);
        final Configurator configurator = Configurator.lookup(k);
        if (configurator == null) throw new IllegalStateException("No configurator implementation to manage "+k);
        return (Describable) configurator.configure(subconfig);
    }

    @Override
    public Describable check(CNode config) throws ConfiguratorException {
        return configure(config);
    }

    private Class findDescribableBySymbol(CNode node, String shortname, List<Descriptor> candidates) {

        // Search for @Symbol annotation on Descriptor to match shortName
        for (Descriptor d : candidates) {
            final List<String> symbols = DescribableAttribute.getSymbols(d, getExtensionPoint(), target);
            final String preferred = symbols.get(0);
            if (preferred.equalsIgnoreCase(shortname)) {
                return d.getKlass().toJavaClass();
            } else {
                for (String symbol : symbols) {
                    if (symbol.equalsIgnoreCase(shortname)) {
                        ObsoleteConfigurationMonitor.get().record(node, "'"+shortname+"' is obsolete, please use '" + preferred + "'");
                        return d.getKlass().toJavaClass();
                    }
                }
            }
        }

        // Not all Describable classes have symbols, give a chance to custom configurators in standalone plugins
        // TODO: probably this logic should have a priority over Symbol so that extensions can override it
        final Configurator c = Configurator.lookupForBaseType(target, shortname);
        if (c != null) {
            Class<?> clazz = c.getTarget();
            if (Describable.class.isAssignableFrom(clazz)) {
                return clazz;
            }
        }

        throw new IllegalArgumentException("No "+target.getName()+ " implementation found for "+shortname);
    }

    @Override
    public Set<Attribute> describe() {
        return Collections.EMPTY_SET;
    }

    @CheckForNull
    @Override
    public CNode describe(Describable instance) throws Exception {
        final String symbol = DescribableAttribute.getPreferredSymbol(instance.getDescriptor(), getTarget(), instance.getClass());
        final Configurator c = Configurator.lookupOrFail(instance.getClass());
        final CNode describe = c.describe(instance);
        if (describe == null) {
            return null;
        }
        if (describe.getType() == CNode.Type.MAPPING && describe.asMapping().size() == 0) {
            return new Scalar(symbol);
        }

        Mapping mapping = new Mapping();
        mapping.put(symbol, describe);
        return mapping;
    }
}
