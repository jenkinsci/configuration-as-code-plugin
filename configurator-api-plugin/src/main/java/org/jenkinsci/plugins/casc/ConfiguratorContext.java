package org.jenkinsci.plugins.casc;

import org.jenkinsci.plugins.casc.settings.ConfiguratorSettings;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.Beta;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.IOException;

/**
 * Holds configuration context
 * @author Oleg Nenashev
 * @since TODO
 */
@Restricted(Beta.class)
public class ConfiguratorContext {

    private final static ThreadLocal<ConfiguratorContext> CONTEXT = new ThreadLocal<>();

    @CheckForNull
    public static ConfiguratorContext get() {
        return CONTEXT.get();
    }

    public static void setContext(@CheckForNull ConfiguratorContext context) {
        CONTEXT.set(context);
    }

    @Nonnull
    private ConfiguratorSettings settings;

    @Nonnull
    private ConfiguratorListener listener;

    public ConfiguratorContext(@Nonnull ConfiguratorSettings settings,
                               @Nonnull ConfiguratorListener listener) {
        this.settings = settings;
        this.listener = listener;
    }

    @Nonnull
    public ConfiguratorSettings getSettings() {
        return settings;
    }

    @Nonnull
    public ConfiguratorListener getListener() {
        return listener;
    }

    public static ConfiguratorContextHolder withContext(@Nonnull ConfiguratorContext context) {
        return new ConfiguratorContextHolder(context);
    }

    @Restricted(Beta.class)
    public static class ConfiguratorContextHolder implements Closeable {

        @CheckForNull
        private ConfiguratorContext oldContext;

        public ConfiguratorContextHolder(@Nonnull ConfiguratorContext context) {
            oldContext = CONTEXT.get();
            CONTEXT.set(context);
        }

        @Override
        public void close() throws IOException {
            CONTEXT.set(oldContext);
        }
    }
}
