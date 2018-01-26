package org.jenkinsci.plugins.casc;

import org.kohsuke.stapler.Stapler;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class PrimitiveConfigurator extends Configurator {
    private final Class target;

    public PrimitiveConfigurator(Class clazz) {
        this.target = clazz;
    }

    @Override
    public Class getTarget() {
        return target;
    }

    @Override
    public Set<Attribute> describe() {
        return Collections.EMPTY_SET;
    }

    @Override
    public Object configure(Object config) throws Exception {
        if (config instanceof String) {
            Optional<String> r = SecretSource.requiresReveal((String) config);
            if(r.isPresent()) {
                Optional<String> reveal = Optional.empty();
                for (SecretSource secretSource : SecretSource.all()) {
                    reveal = secretSource.reveal(r.get());
                    if(reveal.isPresent()) {
                        config = reveal.get();
                        break;
                    }
                }
                if(!reveal.isPresent()) {
                    throw new IllegalArgumentException("Unable to reveal variable with key: "+config);
                }
            }
        }
        return Stapler.lookupConverter(target).convert(target, config);
    }

    @Override
    public List<Configurator> getConfigurators() {
        return Collections.EMPTY_LIST;
    }
}
