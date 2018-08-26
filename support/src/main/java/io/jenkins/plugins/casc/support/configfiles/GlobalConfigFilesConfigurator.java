package io.jenkins.plugins.casc.support.configfiles;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.GlobalConfigFiles;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import hudson.Extension;
import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.BaseConfigurator;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.Configurator;
import io.jenkins.plugins.casc.RootElementConfigurator;
import io.jenkins.plugins.casc.impl.attributes.DescribableAttribute;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import io.jenkins.plugins.casc.model.Sequence;

/**
 * {@link RootElementConfigurator} to handle {@link GlobalConfigFiles} of plugin <code>config-file-provider</code>.
 *
 * @author srempfer
 */
@Extension
@Restricted(NoExternalUse.class)
public class GlobalConfigFilesConfigurator extends BaseConfigurator<GlobalConfigFiles> implements RootElementConfigurator<GlobalConfigFiles> {

    @Override
    public Class<GlobalConfigFiles> getTarget() {
        return GlobalConfigFiles.class;
    }

    @Override
    public GlobalConfigFiles getTargetComponent(ConfigurationContext context) {
        return GlobalConfigFiles.get();
    }

    @Override
    protected GlobalConfigFiles instance(Mapping mapping, ConfigurationContext context) {
        return getTargetComponent(context);
    }

    @Override
    public String getName() {
        return "globalConfigFiles";
    }

    @Override
    public Set<Attribute<GlobalConfigFiles, ?>> describe() {
        Set<Attribute<GlobalConfigFiles, ?>> attributes = new HashSet<>();

        DescribableAttribute<GlobalConfigFiles, Collection<Config>> attribute = new DescribableAttribute<>("configs", Config.class);
        attribute.multiple(true);
        attribute.setter((target, value) -> {
            for (Config config : value) {
                target.save(config);
            }
        });
        attributes.add(attribute);

        return attributes;
    }

    @Override
    public CNode describe(GlobalConfigFiles instance, ConfigurationContext context) throws Exception {
        final Mapping mapping = new Mapping();

        final Configurator cs = context.lookupOrFail(Config.class);

        Sequence seq = new Sequence();
        for (Config config : GlobalConfigFiles.get().getConfigs()) {
            seq.add(cs.describe(config, context));
        }
        mapping.putIfNotEmpry("configs", seq);
        return mapping;
    }

}
