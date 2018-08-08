package io.jenkins.plugins.casc.cli;

import hudson.Extension;
import hudson.cli.CLICommand;
import io.jenkins.plugins.casc.ConfigurationAsCode;
import io.jenkins.plugins.casc.yaml.YamlSource;
import jenkins.model.Jenkins;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import java.io.InputStream;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension
@Restricted(NoExternalUse.class)
public class ApplyConfigurationCommand extends CLICommand {

    @Override
    public String getShortDescription() {
        return "Apply YAML configuration to instance";
    }

    @Override
    protected int run() throws Exception {

        if (!Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER)) {
            return -1;
        }

        ConfigurationAsCode.get().configureWith(new YamlSource<InputStream>(stdin, YamlSource.READ_FROM_INPUTSTREAM));
        return 0;
    }
}
