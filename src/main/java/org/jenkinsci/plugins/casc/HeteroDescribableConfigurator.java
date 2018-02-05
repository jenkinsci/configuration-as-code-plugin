package org.jenkinsci.plugins.casc;

import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.security.HudsonPrivateSecurityRealm;
import hudson.security.SecurityRealm;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
public class HeteroDescribableConfigurator extends Configurator<Describable> {

    private final Class target;

    public HeteroDescribableConfigurator(Class clazz) {
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
    public Describable configure(Object config) throws Exception {
        String shortname;
        Object subconfig = null;
        if (config instanceof String) {
            shortname = (String) config;
        } else if (config instanceof Map) {
            Map<String, ?> map = (Map) config;
            if (map.size() != 1) {
                throw new IllegalArgumentException("single entry map expected to configure a "+target.getName());
            }
            final Map.Entry<String, ?> next = map.entrySet().iterator().next();
            shortname = next.getKey();
            subconfig = next.getValue();
        } else {
            throw new IllegalArgumentException("Unexpected configuration type "+config);
        }

        final List<Descriptor> candidates = Jenkins.getInstance().getDescriptorList(target);

        Class<? extends Describable> k = findDescribableBySymbol(shortname, candidates);
        return (Describable) Configurator.lookup(k).configure(subconfig);
    }

    private Class findDescribableBySymbol(String shortname, List<Descriptor> candidates) {

        // Search for @Symbol annotation on Descriptor to match shortName
        for (Descriptor d : candidates) {
            final String symbol = DescribableAttribute.getSymbolName(d, getExtensionPoint(), target);
            if (symbol.equalsIgnoreCase(shortname)) return d.getKlass().toJavaClass();
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
}
