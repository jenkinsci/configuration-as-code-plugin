package org.jenkinsci.plugins.casc.plugins;

import hudson.model.UpdateSite;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Creates a list of update sites based on a list of key,value pairs. The value is the url for the update center
 */
@Restricted(NoExternalUse.class)
public class UpdateSiteInfo {

    private String id;
    private String url;

    @DataBoundConstructor
    public UpdateSiteInfo(String id, String url) {
        this.id = id;
        this.url = url;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public UpdateSite toUpdateSiteObject() {
        return new UpdateSite(getId(), getUrl());
    }
}
