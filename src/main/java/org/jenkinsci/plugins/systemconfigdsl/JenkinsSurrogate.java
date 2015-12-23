package org.jenkinsci.plugins.systemconfigdsl;

import jenkins.model.Jenkins;

import java.util.ArrayList;
import java.util.List;

/**
 * Root closure delegate that supports pseudo global functions
 * as well as configuring the singleton {@link Jenkins} instance.
 *
 * @author Kohsuke Kawaguchi
 */
public class JenkinsSurrogate extends Surrogate {
    private final Jenkins jenkins;
    private final List<PluginRecipe> recipes = new ArrayList<PluginRecipe>();

    static class PluginRecipe {
        final String name;
        /**
         * Null if no explicit version is specified.
         */
        final String version;


        public PluginRecipe(String name, String version) {
            this.name = name;
            this.version = version;
        }
    }

    public JenkinsSurrogate(Jenkins target) {
        super(target);
        this.jenkins = target;
    }

    /**
     * Installs the latest version of a plugin if it's not present
     */
    public void plugin(String name) {
        plugin(name,null);
    }

    /**
     * Updates a plugin if it's older than the specified version.
     */
    public void plugin(String name, String minimumRequiredVersion) {
        this.recipes.add(new PluginRecipe(name, minimumRequiredVersion));
    }

    /**
     * Run the given closure and configure {@link Jenkins}
     */
    /*package*/ void runWith(ConfigScript s) {
        s.setDelegate(this);
        s.run();
        assign();
    }
}
