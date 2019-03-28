package io.jenkins.plugins.casc.impl.configurators;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.Extension;
import io.jenkins.plugins.casc.BaseConfigurator;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.RootElementConfigurator;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

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

    protected void configure(Mapping config, ConfigurationContext instance, boolean dryrun, ConfigurationContext context) throws ConfiguratorException {
        // ConfigurationContext has to be configured _even_ for dry-run as it determine CasC behaviour
        super.configure(config, instance, false, context);
    }

    @CheckForNull
    @Override
    public CNode describe(ConfigurationContext instance, ConfigurationContext context) throws Exception {
        return compare(instance, new ConfigurationContext(null), context);
    }
}


