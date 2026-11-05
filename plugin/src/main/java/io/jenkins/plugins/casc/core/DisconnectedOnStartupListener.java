package io.jenkins.plugins.casc.core;

import hudson.AbortException;
import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.ComputerListener;
import java.io.IOException;

@SuppressWarnings("unused")
@Extension
public class DisconnectedOnStartupListener extends ComputerListener {

    @Override
    public void preLaunch(Computer c, TaskListener listener) throws IOException {
        Node node = c.getNode();
        if (node == null) {
            return;
        }

        DisconnectedOnStartupProperty prop = node.getNodeProperties().get(DisconnectedOnStartupProperty.class);

        if (prop != null && prop.isEnabled()) {
            listener.getLogger().println("Launch canceled: Node marked to remain disconnected on startup by JCasC.");
            throw new AbortException("JCasC DisconnectedOnStartupProperty prevented launch.");
        }
    }
}
