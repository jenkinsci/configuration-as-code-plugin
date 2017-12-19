package org.jenkinsci.plugins.casc;

import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;

import java.util.List;
import java.util.stream.Collectors;

import static org.jenkinsci.plugins.casc.Configurator.normalize;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class DescribableAttribute<T> extends Attribute<T> {

    public DescribableAttribute(String name, Class<? extends Describable> type) {
        super(name, type);
    }

    @Override
    public List<String> possibleValues() {
        final List<Descriptor> descriptors = Jenkins.getInstance().getDescriptorList(type);
        return descriptors.stream()
                .map(d -> getSymbolName(d, type))
                .collect(Collectors.toList());
    }

    public static String getSymbolName(Descriptor d, Class target) {

        if (d != null) {
            // explicit @Symbol annotation on descriptor
            Symbol s = d.getClass().getAnnotation(Symbol.class);
            if (s != null) return s.value()[0];

            final String sn = target.getSimpleName();
            final String cn = d.getKlass().toJavaClass().getSimpleName();

            // extension type Foo is implemented as SomeFoo. => "some"
            if (cn.endsWith(sn)) {
                return normalize(cn.substring(0, cn.length() - sn.length()));
            }

            // extension type Foo is implemented as SomeFooImpl. => "some"
            final String in = target.getSimpleName() + "Impl";
            if (cn.endsWith(in)) {
                return normalize(cn.substring(0, cn.length() - in.length()));
            }
        }

        // Fall back to simple class name
        return normalize(target.getSimpleName());
    }

}
