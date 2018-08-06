package io.jenkins.plugins.casc;

import hudson.Extension;
import hudson.model.AdministrativeMonitor;
import hudson.util.VersionNumber;
import io.jenkins.plugins.casc.model.Source;
import jenkins.model.Jenkins;
import io.jenkins.plugins.casc.model.CNode;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension
@Restricted(NoExternalUse.class)
public class ObsoleteConfigurationMonitor extends AdministrativeMonitor {

    private final List<Error> errors = new CopyOnWriteArrayList<>();

    @Override
    public boolean isActivated() {
        return !errors.isEmpty();
    }

    public List<Error> getErrors() {
        return errors;
    }


    public void reset() {
        errors.clear();
    }

    public void record(CNode node, String message) {
        errors.add(new Error(node.getSource(), message));
    }

    public static ObsoleteConfigurationMonitor get() {
        return AdministrativeMonitor.all().get(ObsoleteConfigurationMonitor.class);
    }

    public static class Error {
        public final Source source;
        public final String message;

        public Error(Source source, String message) {
            this.source = source;
            this.message = message;
        }
    }

    public String getCss() {
        final VersionNumber version = Jenkins.getVersion();
        if (version == null || version.isNewerThan(new VersionNumber("2.103"))) {
            return "alert alert-warning";
        }
        return "warning";
    }
}
