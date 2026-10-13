package io.jenkins.plugins.casc.core;

import hudson.slaves.OfflineCause;
import java.io.Serializable;

public class JCasCOfflineCause extends OfflineCause implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String reason;

    public JCasCOfflineCause(String reason) {
        this.reason = reason != null ? reason : "Disconnected via JCasC configuration";
    }

    @Override
    public String toString() {
        return "JCasC Startup State: " + reason;
    }
}
