package org.jenkinsci.plugins.casc.cli;

import hudson.Extension;
import hudson.cli.CLICommand;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.casc.ConfigurationAsCode;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension
@Restricted(NoExternalUse.class)
public class ExportConfigurationCommand extends CLICommand {

    @Override
    public String getShortDescription() {
        return "Export jenkins configuration as YAML";
    }

    @Override
    protected int run() throws Exception {

        if (!Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER)) {
            return -1;
        }


        ConfigurationAsCode.get().export(stdout);
        return 0;
    }
}
