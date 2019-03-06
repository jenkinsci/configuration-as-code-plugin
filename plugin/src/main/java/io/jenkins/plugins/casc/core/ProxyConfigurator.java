package io.jenkins.plugins.casc.core;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.ProxyConfiguration;
import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.BaseConfigurator;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.Configurator;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import jenkins.model.Jenkins;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.CheckForNull;

import static io.jenkins.plugins.casc.Attribute.noop;


@Extension
@Restricted(NoExternalUse.class)
public class ProxyConfigurator extends BaseConfigurator<ProxyConfiguration> {

    @Override
    public Class<ProxyConfiguration> getTarget() {
        return ProxyConfiguration.class;
    }

    @Override
    protected void configure(Mapping config, ProxyConfiguration instance, boolean dryrun,
                             ConfigurationContext context) throws ConfiguratorException {
        super.configure(config, instance, dryrun, context);
    }

    @Override
    protected ProxyConfiguration instance(Mapping mapping, ConfigurationContext context) throws ConfiguratorException {
        return Jenkins.getInstance().proxy;
    }

    @NonNull
    @Override
    public Set<Attribute<ProxyConfiguration, ?>> describe() {
        return new HashSet<>(Arrays.asList(
                new Attribute<ProxyConfiguration, String>("name", String.class)
                        .getter(config -> config.name)
                        .setter(noop()),
                new Attribute<ProxyConfiguration, Integer>("port", Integer.class)
                        .getter(config -> config.port)
                        .setter(noop()),
                new Attribute<ProxyConfiguration, String>("userName", String.class)
                        .getter(ProxyConfiguration::getUserName)
                        .setter(noop()),
                new Attribute<ProxyConfiguration, String>("password", String.class)
                        .getter(ProxyConfiguration::getPassword)
                        .setter(noop()),
                new Attribute<ProxyConfiguration, String>("noProxyHost", String.class)
                        .getter(config -> config.noProxyHost)
                        .setter(noop()),
                new Attribute<ProxyConfiguration, String>("testUrl", String.class)
                        .getter(ProxyConfiguration::getTestUrl)
                        .setter(noop())
        ));
    }

    @Override
    @CheckForNull
    public CNode describe(ProxyConfiguration instance, ConfigurationContext context) throws Exception {
        final Mapping mapping = new Mapping();
        final Configurator cp = context.lookupOrFail(ProxyConfiguration.class);
        final ProxyConfiguration proxy = Jenkins.getInstance().proxy;
        if (proxy != null) {
            mapping.putIfNotNull("proxy", cp.describe(proxy, context));
        }
        return mapping;
    }
}
