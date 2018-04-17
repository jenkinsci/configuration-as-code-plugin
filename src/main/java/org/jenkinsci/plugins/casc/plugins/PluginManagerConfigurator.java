package org.jenkinsci.plugins.casc.plugins;

import hudson.Extension;
import hudson.Plugin;
import hudson.PluginManager;
import hudson.PluginWrapper;
import hudson.ProxyConfiguration;
import hudson.model.UpdateCenter;
import hudson.model.UpdateSite;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.casc.Attribute;
import org.jenkinsci.plugins.casc.BaseConfigurator;
import org.jenkinsci.plugins.casc.Configurator;
import org.jenkinsci.plugins.casc.RootElementConfigurator;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension
public class PluginManagerConfigurator extends BaseConfigurator<PluginManager> implements RootElementConfigurator {

    @Override
    public Class<PluginManager> getTarget() {
        return PluginManager.class;
    }

    @Override
    public PluginManager configure(Object config) throws Exception {
        Map<?,?> map = (Map) config;
        final Jenkins jenkins = Jenkins.getInstance();

        final Object proxy = map.get("proxy");
        if (proxy != null) {
            Configurator<ProxyConfiguration> pc = Configurator.lookup(ProxyConfiguration.class);
            ProxyConfiguration pcc = pc.configure(proxy);
            jenkins.proxy = pcc;
        }

        final List<?> sites = (List<?>) map.get("updateSites");
        final UpdateCenter updateCenter = jenkins.getUpdateCenter();
        if (sites != null) {
            Configurator<UpdateSite> usc = Configurator.lookup(UpdateSite.class);
            List<UpdateSite> updateSites = new ArrayList<>();
            for (Object data : sites) {
                UpdateSite in = usc.configure(data);
                updateSites.add(in);
            }

            List<UpdateSite> us = updateCenter.getSites();
            us.clear();
            us.addAll(updateSites);
        }


        final Map<String, String> plugins = (Map) map.get("required");
        if (plugins != null) {
            File shrinkwrap = new File("./plugins-shrinkwrap.yaml");
            if (shrinkwrap.exists()) {
                try (FileReader io = new FileReader(shrinkwrap)) {
                    plugins.putAll(new Yaml().loadAs(io, Map.class));
                }
            }
            final List<UpdateSite.Plugin> requiredPlugins = getRequiredPlugins(plugins, jenkins, updateCenter);
            List<Future<UpdateCenter.UpdateCenterJob>> installations = new ArrayList<>();
            for (UpdateSite.Plugin plugin : requiredPlugins) {
                installations.add(plugin.deploy());
            }

            for (Future<UpdateCenter.UpdateCenterJob> job : installations) {
                // TODO manage failure, timeout, etc
                job.get();
            }

            Map<String, String> installed = new HashMap<>();
            for (PluginWrapper pw : jenkins.getPluginManager().getPlugins()) {
                if (pw.getShortName().equals("configuration-as-code")) continue;
                installed.put(pw.getShortName(), pw.getVersionNumber().toString());
            }
            try (FileWriter w = new FileWriter(shrinkwrap)) {
                w.append(new Yaml().dump(installed));
            }

            if (!installations.isEmpty()) jenkins.restart();
        }

        jenkins.save();
        return jenkins.getPluginManager();
    }

    /**
     * Collect required plugins as {@link UpdateSite.Plugin} so we can trigger installation if required
     */
    private List<UpdateSite.Plugin> getRequiredPlugins(Map<String, String> plugins, Jenkins jenkins, UpdateCenter updateCenter) throws IOException {
        List<UpdateSite.Plugin> installations = new ArrayList<>();
        if (plugins != null) {
            for (Map.Entry<String, String> e : plugins.entrySet()) {
                String shortname = e.getKey();
                String version = e.getValue();
                final Plugin plugin = jenkins.getPlugin(shortname);
                if (plugin == null || !plugin.getWrapper().getVersion().equals(e.getValue())) { // Need to install

                    boolean found = false;
                    for (UpdateSite updateSite : updateCenter.getSites()) {
                        final UpdateSite.Plugin p = updateSite.getPlugin(shortname);
                        if (p != null) {
                            // This plugin is distributed by this update site

                            // FIXME see https://github.com/jenkins-infra/update-center2/pull/192
                            // get plugin metadata for this specific version of the target plugin
                            final URI metadata = URI.create(updateSite.getUrl()).resolve("download/plugins/" + shortname + "/" + version + "/metadata.json");
                            try (InputStream open = ProxyConfiguration.open(metadata.toURL()).getInputStream()) {
                                final JSONObject json = JSONObject.fromObject(IOUtils.toString(open));
                                final UpdateSite.Plugin installable = updateSite.new Plugin(updateSite.getId(), json);
                                installations.add(installable);
                            }
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        throw new IllegalArgumentException("Can't find plugin "+shortname+" in configured update sites");
                    }
                }
            }
        }
        return installations;
    }

    @Override
    public String getName() {
        return "plugins";
    }

    @Override
    public Set<Attribute> describe() {
        Set<Attribute> attr =  new HashSet<>();
        attr.add(new Attribute("proxy", ProxyConfiguration.class));
        attr.add(new Attribute("updateSites", new ArrayList<UpdateSite>().getClass()));
        attr.add(new Attribute("required", new ArrayList<Plugins>().getClass()));
        return attr;
    }
}