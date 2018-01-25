package org.jenkinsci.plugins.casc;

import org.kohsuke.stapler.Stapler;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            String s = (String) config;
            for(SecretSource secretSource : SecretSource.all()) {
                String reveal = secretSource.reveal(s);
                if(reveal != null) {
                    config = reveal;
                    break;
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
