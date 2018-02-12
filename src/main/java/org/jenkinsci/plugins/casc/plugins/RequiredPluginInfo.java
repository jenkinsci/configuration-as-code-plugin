package org.jenkinsci.plugins.casc.plugins;

import hudson.util.VersionNumber;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Created by mads on 2/12/18.
 */
@Restricted(NoExternalUse.class)
public class RequiredPluginInfo {

    private String pluginId;
    private String version;

    @DataBoundConstructor
    public RequiredPluginInfo(String pluginId, String version) {
        this.version = version;
        this.pluginId = pluginId;
    }

    public String getPluginId() {
        return pluginId;
    }

    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

    public String getVersion() {
        return version;
    }

    public VersionNumber toVersionNumberObject() {
        return new VersionNumber(getVersion());
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
