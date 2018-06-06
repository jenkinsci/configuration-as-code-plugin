package org.jenkinsci.plugins.casc;

import hudson.Extension;
import hudson.model.AdministrativeMonitor;
import org.jenkinsci.plugins.casc.model.CNode;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension
public class ObsoleteConfigurationFormat extends AdministrativeMonitor {

    private final List<Error> errors = new ArrayList<>();

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
        errors.add(new Error(node.source(), error));
    }

    public static ObsoleteConfigurationFormat get() {
        return AdministrativeMonitor.all().get(ObsoleteConfigurationFormat.class);
    }

    public static class Error {
        public final String source;
        public final String message;

        public Error(String source, String message) {
            this.source = source;
            this.message = message;
        }
    }
}
