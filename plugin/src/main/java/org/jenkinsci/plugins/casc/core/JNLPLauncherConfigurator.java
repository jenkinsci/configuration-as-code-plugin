package org.jenkinsci.plugins.casc.core;

import hudson.Extension;
import hudson.slaves.JNLPLauncher;
import org.jenkinsci.plugins.casc.ConfiguratorException;
import org.jenkinsci.plugins.casc.impl.configurators.DataBoundConfigurator;
import org.jenkinsci.plugins.casc.model.CNode;
import org.jenkinsci.plugins.casc.model.Mapping;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension
@Restricted(NoExternalUse.class)
public class JNLPLauncherConfigurator extends DataBoundConfigurator<JNLPLauncher> {

    public JNLPLauncherConfigurator() {
        super(JNLPLauncher.class);
    }

    @Override
    protected JNLPLauncher instance(Mapping config) throws ConfiguratorException {
        try {
            return super.instance(config);
        } catch (ConfiguratorException e) {
            // see https://issues.jenkins-ci.org/browse/JENKINS-51603
            final CNode tunnel = config.get("tunnel");
            final CNode vmargs = config.get("vmargs");
            return new JNLPLauncher(tunnel != null ? tunnel.asScalar().getValue() : null,
                                    vmargs != null ? vmargs.asScalar().getValue() : null);
        }
    }
}
