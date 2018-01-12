package org.jenkinsci.plugins.casc;

import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
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
            shortname = map.keySet().iterator().next();
            subconfig = map.get(shortname);
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
            final String symbol = DescribableAttribute.getSymbolName(d, target);
            if (symbol.equalsIgnoreCase(shortname)) return d.getKlass().toJavaClass();
        }

        throw new IllegalArgumentException("No "+target.getName()+ "implementation found for "+shortname);
    }

    @Override
    public Set<Attribute> describe() {
        return Collections.EMPTY_SET;
    }
}
