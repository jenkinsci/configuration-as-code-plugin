package io.jenkins.plugins.casc.core;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.ProxyConfiguration;
import hudson.util.Secret;
import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.BaseConfigurator;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.Configurator;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.model.Mapping;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import static io.jenkins.plugins.casc.Attribute.noop;


@Extension
@Restricted(NoExternalUse.class)
public class ProxyConfigurator extends BaseConfigurator<ProxyConfiguration> {

    @Override
    public Class<ProxyConfiguration> getTarget() {
        return ProxyConfiguration.class;
    }

    @NonNull
    @Override
    public Class getImplementedAPI() {
        return ProxyConfigurationDataBounded.class;
    }

    @Override
    protected ProxyConfiguration instance(Mapping mapping, ConfigurationContext context) throws ConfiguratorException {
        final Configurator<ProxyConfigurationDataBounded> c = context.lookupOrFail(ProxyConfigurationDataBounded.class);
        final ProxyConfigurationDataBounded proxy = c.configure(mapping, context);
        return new ProxyConfiguration(proxy.name, proxy.port, proxy.userName,
            Secret.toString(proxy.password), proxy.noProxyHost, proxy.testUrl);
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

    @Restricted(NoExternalUse.class)
    public static class ProxyConfigurationDataBounded {
        private final String name;
        private final int port;
        private String userName;
        private String noProxyHost;
        private Secret password;
        private String testUrl;

        @DataBoundConstructor
        public ProxyConfigurationDataBounded(String name, int port) {
            this.name = name;
            this.port = port;
        }

        public String getName() {
            return name;
        }

        public int getPort() {
            return port;
        }

        public String getUserName() {
            return userName;
        }

        @DataBoundSetter
        public void setUserName(String userName) {
            this.userName = userName;
        }

        public String getNoProxyHost() {
            return noProxyHost;
        }

        @DataBoundSetter
        public void setNoProxyHost(String noProxyHost) {
            this.noProxyHost = noProxyHost;
        }

        public Secret getPassword() {
            return password;
        }

        @DataBoundSetter
        public void setPassword(Secret password) {
            this.password = password;
        }

        public String getTestUrl() {
            return testUrl;
        }

        @DataBoundSetter
        public void setTestUrl(String testUrl) {
            this.testUrl = testUrl;
        }
    }
}
