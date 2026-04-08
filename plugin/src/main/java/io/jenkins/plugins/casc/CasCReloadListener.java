package io.jenkins.plugins.casc;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface CasCReloadListener extends ExtensionPoint {

    void onConfigurationReloaded();

    static void fire() {
        Logger logger = Logger.getLogger(CasCReloadListener.class.getName());

        for (CasCReloadListener listener : ExtensionList.lookup(CasCReloadListener.class)) {
            try {
                listener.onConfigurationReloaded();
            } catch (Exception e) {
                logger.log(
                        Level.WARNING,
                        "Listener " + listener.getClass().getName()
                                + " threw an exception during CasC reload notification",
                        e);
            }
        }
    }
}
