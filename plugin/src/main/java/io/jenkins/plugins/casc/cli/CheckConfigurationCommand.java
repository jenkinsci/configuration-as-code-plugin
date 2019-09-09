package io.jenkins.plugins.casc.cli;

import hudson.Extension;
import hudson.cli.CLICommand;
import io.jenkins.plugins.casc.ConfigurationAsCode;
import io.jenkins.plugins.casc.model.Source;
import io.jenkins.plugins.casc.yaml.YamlSource;
import java.util.Map;
import jenkins.model.Jenkins;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension
@Restricted(NoExternalUse.class)
public class CheckConfigurationCommand extends CLICommand {

    @Override
    public String getShortDescription() {
        return "Check YAML configuration to instance";
    }

    @Override
    protected int run() throws Exception {

        if (!Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER)) {
            return -1;
        }

        final Map<Source, String> issues = ConfigurationAsCode.get().checkWith(YamlSource.of(stdin));
        for (Map.Entry<Source, String> entry : issues.entrySet()) {
            stderr.printf("warning: line %d %s", entry.getKey().line, entry.getValue());
        }
        return 0;
    }
}
