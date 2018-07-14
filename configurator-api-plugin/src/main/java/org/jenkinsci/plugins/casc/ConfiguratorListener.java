package org.jenkinsci.plugins.casc;

import org.jenkinsci.plugins.casc.model.CNode;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.Beta;

import javax.annotation.Nonnull;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Listens for configurator events.
 * @author Oleg Nenashev
 */
@Restricted(Beta.class)
public interface ConfiguratorListener {

    void onError(@Nonnull CNode node, @Nonnull String error);

    @Restricted(Beta.class)
    class LoggerConfiguratorListener implements ConfiguratorListener {
        Logger logger;
        Level errorLevel;

        public LoggerConfiguratorListener(@Nonnull Logger logger, @Nonnull Level errorLevel) {
            this.logger = logger;
            this.errorLevel = errorLevel;
        }

        @Override
        public void onError(@Nonnull CNode node, @Nonnull String error) {
            logger.log(errorLevel, error);
        }
    }
}
