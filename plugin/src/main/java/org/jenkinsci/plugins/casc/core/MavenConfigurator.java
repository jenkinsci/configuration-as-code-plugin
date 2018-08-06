package org.jenkinsci.plugins.casc.core;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.tasks.Maven;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import jenkins.mvn.GlobalMavenConfig;
import org.jenkinsci.plugins.casc.Attribute;
import org.jenkinsci.plugins.casc.BaseConfigurator;
import org.jenkinsci.plugins.casc.ConfigurationContext;
import org.jenkinsci.plugins.casc.Configurator;
import org.jenkinsci.plugins.casc.impl.configurators.DescriptorConfigurator;
import org.jenkinsci.plugins.casc.model.CNode;
import org.jenkinsci.plugins.casc.model.Mapping;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.Set;

/**
 *  A mix-in configurator to support both {@link Maven.DescriptorImpl} and {@link GlobalMavenConfig} which both are are
 *  {@link Descriptor}s for the "tools" category using a conflicting {@link org.jenkinsci.Symbol}.
 *  <p>
 *  We can't blame them for this, as the former is a {@link hudson.tasks.BuildStepDescriptor} while the later is a
 *  {@link GlobalConfiguration}, so from their point of view the symbol is unique for implemented extension point. Just
 *  we don't distinguish dreived classes from {@link Descriptor} as distinct APIs.
 *
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
        final Configurator<Descriptor> task = new DescriptorConfigurator(descriptor);

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
    public CNode describe(GlobalMavenConfig instance, ConfigurationContext context) throws Exception {
        return null;
    }
}
