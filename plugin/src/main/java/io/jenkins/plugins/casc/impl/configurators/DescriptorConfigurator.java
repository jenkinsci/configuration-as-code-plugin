package io.jenkins.plugins.casc.impl.configurators;

import com.google.common.base.CaseFormat;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Descriptor;
import io.jenkins.plugins.casc.BaseConfigurator;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.RootElementConfigurator;
import io.jenkins.plugins.casc.model.Mapping;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.jenkinsci.Symbol;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import static java.util.Collections.singletonList;

/**
 * Define a Configurator for a Descriptor
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Restricted(NoExternalUse.class)
public class DescriptorConfigurator extends BaseConfigurator<Descriptor> implements RootElementConfigurator<Descriptor> {


    private final List<String> names;
    private final Descriptor descriptor;
    private final Class target;

    public DescriptorConfigurator(Descriptor descriptor) {
        this.descriptor = descriptor;
        this.target = descriptor.getClass();
        this.names = resolvePossibleNames(descriptor);
    }

    @NonNull
    @Override
    public String getName() {
        return names.get(0);
    }

    @NonNull
    @Override
    public List<String> getNames() {
        return names;
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

    private List<String> resolvePossibleNames(Descriptor descriptor) {
        return Optional.ofNullable(descriptor.getClass().getAnnotation(Symbol.class))
                .map(s -> Arrays.asList(s.value()))
                .orElseGet(() -> {
                    /* TODO: extract Descriptor parameter type such that DescriptorImpl extends Descriptor<XX> returns XX.
                     * Then, if `baseClass == fooXX` we get natural name `foo`.
                     */
                    return singletonList(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, descriptor.getKlass().toJavaClass().getSimpleName()));
                });
    }
}
