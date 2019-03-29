package io.jenkins.plugins.casc;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

/**
 *
 */
@Extension
public class CasCGlobalConfig extends GlobalConfiguration {

    private String configurationPath;

    @DataBoundConstructor
    public CasCGlobalConfig(String configurationPath) {
        this.configurationPath = configurationPath;
    }

    public CasCGlobalConfig() {
        load();
    }

    @NonNull
    @Override
    public String getDisplayName() {
        return "CasC configuration";
    }

    public String getConfigurationPath() {
        return configurationPath;
    }

    @DataBoundSetter
    public void setConfigurationPath(String configurationPath) {
        this.configurationPath = configurationPath;
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        req.bindJSON(this, json);
        save();
        return super.configure(req, json);
    }

}
