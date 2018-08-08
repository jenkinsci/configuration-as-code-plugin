package io.jenkins.plugins.casc.impl.configurators;

import hudson.model.Descriptor;
import io.jenkins.plugins.casc.BaseConfigurator;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.RootElementConfigurator;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import org.jenkinsci.Symbol;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import javax.annotation.CheckForNull;

/**
 * Define a Configurator for a Descriptor
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Restricted(NoExternalUse.class)
public class DescriptorConfigurator extends BaseConfigurator<Descriptor> implements RootElementConfigurator<Descriptor> {


    private final String name;
    private final Descriptor descriptor;
    private final Class target;

    public DescriptorConfigurator(Descriptor descriptor) {
        this.descriptor = descriptor;
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
    public Descriptor getTargetComponent(ConfigurationContext context) {
        return descriptor;
    }

    @Override
    protected Descriptor instance(Mapping mapping, ConfigurationContext context) {
        return descriptor;
    }

    @CheckForNull
    @Override
    public CNode describe(Descriptor instance, ConfigurationContext context) throws Exception {
        final Descriptor ref = (Descriptor) target.newInstance();
        return compare(instance, ref, context);
    }


}
