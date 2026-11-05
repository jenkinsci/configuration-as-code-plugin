package io.jenkins.plugins.casc.core;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Node;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class DisconnectedOnStartupProperty extends NodeProperty<Node> {

    private String reason;
    private boolean enabled = true;

    @DataBoundConstructor
    public DisconnectedOnStartupProperty() {}

    @DataBoundSetter
    public void setReason(String reason) {
        this.reason = reason != null ? reason.trim() : null;
    }

    @DataBoundSetter
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getReason() {
        return (reason != null && !reason.trim().isEmpty()) ? reason : "Disconnected via JCasC configuration";
    }

    @Extension
    @Symbol("disconnectedOnStartup")
    public static class DescriptorImpl extends NodePropertyDescriptor {
        @Override
        @NonNull
        public String getDisplayName() {
            return "Disconnect Node on Startup";
        }
    }
}
