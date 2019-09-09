package io.jenkins.plugins.casc.core;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.UpdateCenter;
import hudson.model.UpdateSite;
import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.BaseConfigurator;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.impl.attributes.MultivaluedAttribute;
import io.jenkins.plugins.casc.model.Mapping;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import jenkins.model.Jenkins;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;


/**
 * TODO would  not be required if UpdateCenter had a DataBoundConstructor
 */
@Extension
@Restricted(NoExternalUse.class)
public class UpdateCenterConfigurator extends BaseConfigurator<UpdateCenter> {

    @Override
    public Class<UpdateCenter> getTarget() {
        return UpdateCenter.class;
    }

    @Override
    protected void configure(Mapping config, UpdateCenter instance, boolean dryrun,
        ConfigurationContext context) throws ConfiguratorException {
        super.configure(config, instance, dryrun, context);
    }

    @Override
    protected UpdateCenter instance(Mapping mapping, ConfigurationContext context) throws ConfiguratorException {
        return Jenkins.getInstance().getUpdateCenter();
    }

    @Override
    @NonNull
    public Set<Attribute<UpdateCenter, ?>> describe() {
        return new HashSet<>(Collections.singletonList(
            new MultivaluedAttribute<UpdateCenter, UpdateSite>("sites", UpdateSite.class)
                .getter(UpdateCenter::getSiteList)
                .setter((target, value) -> target.getSites().replaceBy(value))
        ));
    }
}
