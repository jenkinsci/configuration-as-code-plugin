package org.jenkinsci.plugins.casc.plugins;

import hudson.Extension;
import hudson.model.UpdateSite;
import org.jenkinsci.plugins.casc.BaseConfigurator;
import org.jenkinsci.plugins.casc.ConfiguratorException;
import org.jenkinsci.plugins.casc.model.CNode;
import org.jenkinsci.plugins.casc.model.Mapping;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.accmod.Restricted;

import javax.annotation.CheckForNull;


/**
 * TODO would  not be required if UpdateSite had a DataBoundConstructor
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension
@Restricted(NoExternalUse.class)
public class UpdateSiteConfigurator extends BaseConfigurator<UpdateSite> {

    @Override
    public Class<UpdateSite> getTarget() {
        return UpdateSite.class;
    }

    @Override
    protected UpdateSite instance(Mapping mapping) throws ConfiguratorException {
        return new UpdateSite(mapping.getScalarValue("id"), mapping.getScalarValue("url"));
    }

    @CheckForNull
    @Override
    public CNode describe(UpdateSite instance) throws Exception {
        final Mapping mapping = new Mapping();
        // TODO would need to compare with hudson.model.UpdateCenter.createDefaultUpdateSite
        // so we return null if default update site is in use.
        mapping.put("id", instance.getId());
        mapping.put("url", instance.getUrl());
        return mapping;
    }
}
