package org.jenkinsci.plugins.casc.impl.configurators;

import hudson.Extension;
import org.jenkinsci.plugins.casc.BaseConfigurator;
import org.jenkinsci.plugins.casc.ConfigurationContext;
import org.jenkinsci.plugins.casc.RootElementConfigurator;
import org.jenkinsci.plugins.casc.model.CNode;
import org.jenkinsci.plugins.casc.model.Mapping;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import javax.annotation.CheckForNull;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension(ordinal = Double.MAX_VALUE)
@Restricted(NoExternalUse.class)
public class SelfConfigurator extends BaseConfigurator<ConfigurationContext> implements RootElementConfigurator<ConfigurationContext> {

    @Override
    public String getName() {
        return "configuration-as-code";
    }

    @Override
    public Class<ConfigurationContext> getTarget() {
        return ConfigurationContext.class;
    }

    @Override
    public ConfigurationContext getTargetComponent(ConfigurationContext context) {
        return context;
    }

    @Override
    protected ConfigurationContext instance(Mapping mapping, ConfigurationContext context) {
        return context;
    }

    @CheckForNull
    @Override
    public CNode describe(ConfigurationContext instance) throws Exception {
        return compare(instance, new ConfigurationContext());
    }
}


