package org.jenkinsci.plugins.casc;

import hudson.ExtensionList;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;

import java.util.Map;
import java.util.Set;

/**
 * Define a Configurator for a Descriptor
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class DescriptorRootElementConfigurator extends BaseConfigurator<Descriptor> implements RootElementConfigurator {


    private final String name;
    private final Class target;

    public DescriptorRootElementConfigurator(Descriptor descriptor) {
        this.target = descriptor.getClass();
        final Symbol symbol = descriptor.getClass().getAnnotation(Symbol.class);
        if (symbol != null) {
            this.name = symbol.value()[0];
        } else {
            final String cl = descriptor.getKlass().toJavaClass().getSimpleName();
            // TODO extract Descriptor parameter type, ie DescriptorImpl extends Descriptor<XX> -> XX
            // so that if cl = fooXX we get natural name "foo"
            this.name = cl.toLowerCase();
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Class<Descriptor> getTarget() {
        return target;
    }

    @Override
    public Descriptor configure(Object config) throws Exception {
        final ExtensionList extensionList = Jenkins.getInstance().getExtensionList(target);
        if (extensionList.size() != 1) {
            throw new IllegalStateException("Failed to retrieve Descriptor "+ target);
        }
        Descriptor d = (Descriptor) extensionList.get(0);configure((Map) config, d);
        return d;
    }

}
