package org.jenkinsci.plugins.casc.core;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.tasks.Maven;
import jenkins.model.Jenkins;
import jenkins.mvn.GlobalMavenConfig;
import org.jenkinsci.plugins.casc.Attribute;
import org.jenkinsci.plugins.casc.BaseConfigurator;
import org.jenkinsci.plugins.casc.ConfigurationContext;
import org.jenkinsci.plugins.casc.Configurator;
import org.jenkinsci.plugins.casc.model.CNode;
import org.jenkinsci.plugins.casc.model.Mapping;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.Set;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension
@Restricted(NoExternalUse.class)
public class MavenConfigurator extends BaseConfigurator<GlobalMavenConfig> {


    @Override
    public Class<GlobalMavenConfig> getTarget() {
        return GlobalMavenConfig.class;
    }

    @Override
    protected GlobalMavenConfig instance(Mapping mapping, ConfigurationContext context) {
        return GlobalMavenConfig.get();
    }

    @Nonnull
    @Override
    public Set<Attribute> describe() {
        final Set<Attribute> attributes = super.describe();
        final Descriptor descriptor = Jenkins.getInstance().getDescriptorOrDie(Maven.class);
        final Configurator<Descriptor> task = Configurator.lookup(descriptor.getClass());
        if (task == null) return attributes; // ?

        for (Attribute attribute : task.describe()) {
            attributes.add(new Attribute(attribute.getName(), attribute.getType())
                .multiple(attribute.isMultiple())
                .getter(g -> attribute.getValue(descriptor))
                .setter((g,v) -> attribute.setValue(descriptor,v)));
        }
        return attributes;
    }

    @CheckForNull
    @Override
    public CNode describe(GlobalMavenConfig instance) throws Exception {
        return null;
    }
}
