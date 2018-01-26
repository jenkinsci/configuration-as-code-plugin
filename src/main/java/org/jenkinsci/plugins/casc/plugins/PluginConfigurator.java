package org.jenkinsci.plugins.casc.plugins;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.PluginManager;
import hudson.PluginWrapper;
import hudson.ProxyConfiguration;
import hudson.model.UpdateSite;
import hudson.util.VersionNumber;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.casc.Attribute;
import org.jenkinsci.plugins.casc.Configurator;
import org.jenkinsci.plugins.casc.RootElementConfigurator;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created by mads on 1/29/18.
 */
@Extension(ordinal = Double.MAX_VALUE)
public class PluginConfigurator implements RootElementConfigurator {

    private static final Logger LOGGER = Logger.getLogger(PluginConfigurator.class.getName());

    @Override
    public PluginManager configure(Object config) throws Exception {
        Map<?,?> map = (Map) config;

        //Proxy is optional
        if(map.containsKey("proxy")) {
            Configurator<ProxyConfiguration> pc = Configurator.lookup(ProxyConfiguration.class);
            ProxyConfiguration pcc = pc.configure(map.get("proxy"));
            Jenkins.getInstance().proxy = pcc;
        }

        HashMap<String, UpdateSite> allUpdateSites = new HashMap<>();
        Configurator<UpdateSiteInfo> configUpdateInfo = Configurator.lookup(UpdateSiteInfo.class);

        //Reuse the default update center
        List<UpdateSite> sites = Jenkins.getInstance().getUpdateCenter().getSites();
        for(UpdateSite us : sites) {
            if(us.getId().equals("default")) {
                allUpdateSites.put(us.getId(), us);
            }
        }

        //Extra sites from configuration
        UpdateSiteInfo in = configUpdateInfo.configure(map.get("updateSites"));
        allUpdateSites.putAll(in);

        //Clear all
        sites.clear();

        //Add ones from configuration
        sites.addAll(allUpdateSites.values());
        Jenkins.getInstance().save();

        //Do check to see if we have installed the required plugins
        StringBuilder existingPlugins = new StringBuilder();
        Configurator<VersionNumber> vnc = Configurator.lookup(VersionNumber.class);
        HashMap<String,VersionNumber> requiredPlugins = new HashMap<>();

        //Required plugins list is optional
        if(map.containsKey("required")) {
            for (Map.Entry reqPlug : ((Map<?, ?>) map.get("required")).entrySet()) {
                requiredPlugins.put((String) reqPlug.getKey(), vnc.configure(reqPlug.getValue()));
            }
        }

        //TODO: Much of this will change. Until we get a proper way to install plugins
        List<String> pluginsToInstall = new ArrayList<>();
        for(Map.Entry<String,VersionNumber> requiredPlugin : requiredPlugins.entrySet()) {
            PluginWrapper plugin = Jenkins.getInstance().getPluginManager().getPlugin(requiredPlugin.getKey());
            if(plugin == null) {
                LOGGER.info("Missing plugin: "+requiredPlugin.getKey()+". Adding to list of plugins to install");
                pluginsToInstall.add(requiredPlugin.getKey());
            } else if (plugin.getVersionNumber().isOlderThan(requiredPlugin.getValue())) {
                existingPlugins.append(String.format("Required plugin %s(%s) is older than the required version: %s",
                        plugin.getShortName(),
                        plugin.getVersion(),
                        requiredPlugin.getValue()) + "\n");
            } else {
                LOGGER.info(String.format("Plugin '%s' is up to date for configuration-as-code plugin", requiredPlugin.getKey()));
            }
        }

        //Install missing plugins
        Jenkins.getInstance().getPluginManager().install(pluginsToInstall, true);

        //Restart if necessary
        if(Jenkins.getInstance().getUpdateCenter().isRestartRequiredForCompletion()) {
            Jenkins.getInstance().restart();
        }

        if(!StringUtils.isBlank(existingPlugins.toString())) {
            //TODO: What is the proper reaction?
            throw new RuntimeException("Missing plugins detected: \n" + existingPlugins.toString());
        }

        return Jenkins.getInstance().getPluginManager();
    }

    @Override
    public String getName() {
        return "plugins";
    }

    /**
     * A set of fake attributes for the PluginManager. When configuring plugins we need the PluginManager to find
     * installed plugins but to update UpdateSites we need to configure the UpdateCenter list on the Jenkins instance.
     * @return
     */
    @Override
    @SuppressFBWarnings(value="DM_NEW_FOR_GETCLASS", justification="one can't get a parameterized type .class")
    public Set<Attribute> describe() {
        Set<Attribute> attr =  new HashSet<Attribute>();
        attr.add(new Attribute("proxy", ProxyConfiguration.class));
        attr.add(new Attribute("updateSites", new HashMap<String,UpdateSite>().getClass()));
        attr.add(new Attribute("required", new HashMap<String,VersionNumber>().getClass()));
        return attr;
    }
}
