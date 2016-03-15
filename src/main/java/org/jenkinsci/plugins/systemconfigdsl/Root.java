package org.jenkinsci.plugins.systemconfigdsl;

import groovy.lang.Closure;
import hudson.PluginManager.FailedPlugin;
import hudson.PluginWrapper;
import hudson.lifecycle.Lifecycle;
import hudson.model.Descriptor;
import hudson.util.VersionNumber;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.structs.SymbolLookup;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

/**
 * Closure delegate to configure the root scope.
 *
 * This defines sub-scopes like 'plugin' and 'jenkins'
 *
 * @author Kohsuke Kawaguchi
 */
public class Root extends ConfiguringObject {
    private final List<PluginRecipe> recipes = new ArrayList<>();
    private final List<Callable> configs = new ArrayList<>();
    private final Surrogate jenkinsSurrogate;
    private final Jenkins jenkins;

    private boolean updateCenterMetadataFetched = false;

    class PluginRecipe {
        final String name;
        /**
         * Null if no explicit version is specified.
         */
        final VersionNumber version;


        public PluginRecipe(String name, VersionNumber version) {
            this.name = name;
            this.version = version;
        }

        /**
         * Ensures that the plugin requested is present by installing it if necessary
         */
        public void perform() throws Exception {
            if (requiresInstallation()) {
                install();
            } else {
                // if we are not installing/updating a plugin, it better be functioning
                for (FailedPlugin f : Jenkins.getInstance().getPluginManager().getFailedPlugins()) {
                    if (f.name.equals(name)) {
                        throw new Error("Plugin "+name+" failed to start", f.cause);
                    }
                }
            }
        }

        /**
         * Checks if we need to install a plugin here
         */
        private boolean requiresInstallation() {
            PluginWrapper n = Jenkins.getInstance().getPluginManager().getPlugin(name);
            // plugin doesn't exist at all?
            if (n==null)    return true;

            // plugin exists and user is happy with any version?
            if (version==null)  return false;

            // the current version is older than what's specified?
            return n.getVersionNumber().isOlderThan(version);
        }

        /**
         * Synchronously installs a plugin and throws an exception if the installation fails.
         */
        private void install() throws Exception {
            if (!updateCenterMetadataFetched) {
                jenkins.pluginManager.doCheckUpdatesServer();
                updateCenterMetadataFetched = true;
            }
            jenkins.getUpdateCenter().getPlugin(name).deploy(true).get();
        }
    }

    public Root(Jenkins jenkins) {
        this.jenkins = jenkins;
        this.jenkinsSurrogate = new Surrogate(jenkins);
    }

    /**
     * Installs the latest version of a plugin if it's not present
     */
    public void plugin(String name) {
        this.recipes.add(new PluginRecipe(name, null));
    }

    /**
     * Updates a plugin if it's older than the specified version.
     */
    public void plugin(String name, String minimumRequiredVersion) {
        this.recipes.add(new PluginRecipe(name, new VersionNumber(minimumRequiredVersion)));
    }

    /**
     * Configures the root {@link Jenkins} instance.
     */
    public void jenkins(final Closure config) {
        configs.add(new Callable() {
            @Override
            public Object call() throws Exception {
                // configure root Jenkins
                jenkinsSurrogate.runWith(config);
                return null;
            }
        });
    }

    /**
     * Configures {@link GlobalConfiguration} by running a given closure.
     */
    public void global(final String name, final Closure config) {
        configure(name, GlobalConfiguration.class, config);
    }

    /**
     * Configures {@link Descriptor} by running a given closure.
     */
    public void descriptor(final String name, final Closure config) {
        configure(name, Descriptor.class, config);
    }

    private void configure(final String name, final Class type, final Closure config) {
        configs.add(new Callable() {
            @Override
            public Object call() throws Exception {
                Surrogate s = new Surrogate(SymbolLookup.get().find(type, name));
                s.runWith(config);
                return null;
            }
        });
    }

    /*package*/ void assign(Jenkins jenkins) throws Exception {
        for (PluginRecipe p : recipes) {
            p.perform();
        }
        if (jenkins.getUpdateCenter().isRestartRequiredForCompletion()) {
            Lifecycle l = Lifecycle.get();
            if (l.canRestart()) {
                // restart Jenkins to load up new plugins
                l.restart();
                return;
            } else {
                LOGGER.severe("Need to start again to have the newly installed plugins take effect. Quitting");
                System.exit(1);
            }
        }

        // apply the rest of the configuration
        for (Callable c : configs) {
            c.call();
        }
    }

    private static final Logger LOGGER = Logger.getLogger(Root.class.getName());
}
