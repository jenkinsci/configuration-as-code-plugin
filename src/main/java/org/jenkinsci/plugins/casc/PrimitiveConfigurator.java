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

    public static final Pattern ENV_VARIABLE = Pattern.compile("\\$\\{(.*)\\}");

    @Override
    public Object configure(Object config) throws Exception {
        if (config instanceof String) {
            String s = (String) config;
            // TODO I Wonder this could be done during parsing with some snakeyml extension
            final Matcher matcher = ENV_VARIABLE.matcher(s);
            if (matcher.matches()) {
                final String var = matcher.group(1);
                config = System.getenv(var);
                if (config == null) throw new IllegalStateException("Environment variable not set: "+var);
            }
        }
        return Stapler.CONVERT_UTILS.convert(config, target);
    }


    @Override
    public List<Configurator> getConfigurators() {
        return Collections.EMPTY_LIST;
    }
}
