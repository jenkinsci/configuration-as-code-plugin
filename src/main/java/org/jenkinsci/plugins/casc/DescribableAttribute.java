package org.jenkinsci.plugins.casc;

import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class DescribableAttribute extends Attribute {

    public DescribableAttribute(String name, Class<? extends Describable> type) {
        super(name, type);
    }

    @Override
    public List<String> possibleValues() {
        final List<Descriptor> descriptors = Jenkins.getInstance().getDescriptorList(type);
        return descriptors.stream()
                .map(d -> {
                    Symbol s = (Symbol) d.getClass().getAnnotation(Symbol.class);
                    if (s != null) return s.value()[0];
                    // TODO truncate extension class name, so LegacyAuthorizationStrategy => "Legacy"
                    else return d.getKlass().toJavaClass().getSimpleName();
                })
                .collect(Collectors.toList());
    }
}
