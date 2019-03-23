package io.jenkins.plugins.casc.impl.configurators;

import com.google.common.base.CaseFormat;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Descriptor;
import io.jenkins.plugins.casc.BaseConfigurator;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.RootElementConfigurator;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import org.jenkinsci.Symbol;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import java.util.Optional;

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
        this.name = resolveName(descriptor);
    }

    @NonNull
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

    private String resolveName(Descriptor descriptor) {
        return Optional.ofNullable(descriptor.getClass().getAnnotation(Symbol.class))
                .map(s -> s.value()[0])
                .orElseGet(() -> {
                    /* TODO: extract Descriptor parameter type such that DescriptorImpl extends Descriptor<XX> returns XX.
                     * Then, if `baseClass == fooXX` we get natural name `foo`.
                     */
                    return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, descriptor.getKlass().toJavaClass().getSimpleName());
                });
    }
}
