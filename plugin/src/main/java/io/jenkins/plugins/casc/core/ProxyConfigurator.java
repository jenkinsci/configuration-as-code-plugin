package io.jenkins.plugins.casc.core;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.ProxyConfiguration;
import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.BaseConfigurator;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.SecretSourceResolver;
import io.jenkins.plugins.casc.model.Mapping;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

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
        Set<String> keys = mapping.keySet();
        return new ProxyConfiguration(
                SecretSourceResolver.resolve(context, mapping.getScalarValue("name")),
                Integer.parseInt(SecretSourceResolver.resolve(context, mapping.getScalarValue("port"))),
                getOptionalParam(mapping, context, keys, "userName"),
                getOptionalParam(mapping, context, keys, "password"),
                getOptionalParam(mapping, context, keys, "noProxyHost"),
                getOptionalParam(mapping, context, keys, "testUrl")
        );
    }

    private String getOptionalParam(Mapping mapping, ConfigurationContext context,
                                    Set<String> keys, String key) throws ConfiguratorException {
        return keys.contains(key) ? SecretSourceResolver.resolve(context, mapping.getScalarValue(key)) : null;
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
                        .getter(ProxyConfiguration::getEncryptedPassword)
                        .setter(noop()),
                new Attribute<ProxyConfiguration, String>("noProxyHost", String.class)
                        .getter(config -> config.noProxyHost)
                        .setter(noop()),
                new Attribute<ProxyConfiguration, String>("testUrl", String.class)
                        .getter(ProxyConfiguration::getTestUrl)
                        .setter(noop())
        ));
    }
}
