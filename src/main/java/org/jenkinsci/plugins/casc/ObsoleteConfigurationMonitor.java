package org.jenkinsci.plugins.casc;

import hudson.Extension;
import hudson.model.AdministrativeMonitor;
import hudson.util.VersionNumber;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.casc.model.CNode;
import org.jenkinsci.plugins.casc.model.Source;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension
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

    public void record(CNode node, String error) {
        errors.add(new Error(node.getSource(), error));
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
