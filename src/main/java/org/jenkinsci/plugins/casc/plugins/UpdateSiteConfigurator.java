package org.jenkinsci.plugins.casc.plugins;

import hudson.Extension;
import hudson.model.UpdateSite;
import org.jenkinsci.plugins.casc.BaseConfigurator;

import java.util.Map;

/**
 * TODO would  not be required if UpdateSite had a DataBoundConstructor
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension
public class UpdateSiteConfigurator extends BaseConfigurator<UpdateSite> {

    @Override
    public Class<UpdateSite> getTarget() {
        return UpdateSite.class;
    }

    @Override
    public UpdateSite configure(Object config) {
        Map<String, String> map = (Map) config;
        return new UpdateSite(map.get("id"), map.get("url"));
    }
}
