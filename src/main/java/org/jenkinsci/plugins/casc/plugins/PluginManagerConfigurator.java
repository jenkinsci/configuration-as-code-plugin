package org.jenkinsci.plugins.casc.plugins;

import hudson.Extension;
import hudson.Plugin;
import hudson.PluginManager;
import hudson.PluginWrapper;
import hudson.ProxyConfiguration;
import hudson.lifecycle.RestartNotSupportedException;
import hudson.model.UpdateCenter;
import hudson.model.UpdateSite;
import hudson.util.PersistedList;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.casc.Attribute;
import org.jenkinsci.plugins.casc.BaseConfigurator;
import org.jenkinsci.plugins.casc.Configurator;
import org.jenkinsci.plugins.casc.ConfiguratorException;
import org.jenkinsci.plugins.casc.RootElementConfigurator;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension(ordinal = 999)
public class PluginManagerConfigurator extends BaseConfigurator<PluginManager> implements RootElementConfigurator<PluginManager> {

    @Override
    public Class<PluginManager> getTarget() {
        return PluginManager.class;
    }

    @Override
    public PluginManager configure(Object config) throws ConfiguratorException {
        Map<?,?> map = (Map) config;
        final Jenkins jenkins = Jenkins.getInstance();

        final Object proxy = map.get("proxy");
        if (proxy != null) {
            Configurator<ProxyConfiguration> pc = Configurator.lookup(ProxyConfiguration.class);
            if (pc == null) throw new ConfiguratorException("ProxyConfiguration not well registered");
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
                if (in.isDue()) {
                    in.updateDirectly(true);
                }
                updateSites.add(in);
            }

            try {
                updateCenter.getSites().replaceBy(updateSites);
            } catch (IOException e) {
                throw new ConfiguratorException("failed to reconfigure updateCenter.sites", e);
            }
        }


        final Map<String, String> plugins = (Map) map.get("required");
        if (plugins != null) {
            File shrinkwrap = new File("./plugins-shrinkwrap.yaml");
            if (shrinkwrap.exists()) {
                try (Reader io = new InputStreamReader(new FileInputStream(shrinkwrap), StandardCharsets.UTF_8)) {
                    plugins.putAll(new Yaml().loadAs(io, Map.class));
                } catch (IOException e) {
                    throw new ConfiguratorException("failed to load shrinkwrap file", e);
                }
            }
            final List<UpdateSite.Plugin> requiredPlugins = getRequiredPlugins(plugins, jenkins, updateCenter);
            List<Future<UpdateCenter.UpdateCenterJob>> installations = new ArrayList<>();
            for (UpdateSite.Plugin plugin : requiredPlugins) {
                installations.add(plugin.deploy());
            }

            for (Future<UpdateCenter.UpdateCenterJob> job : installations) {
                // TODO manage failure, timeout, etc
                try {
                    job.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new ConfiguratorException("interrupted while waiting for plugins installation", e);
                }
            }

            Map<String, String> installed = new HashMap<>();
            for (PluginWrapper pw : jenkins.getPluginManager().getPlugins()) {
                if (pw.getShortName().equals("configuration-as-code")) continue;
                installed.put(pw.getShortName(), pw.getVersionNumber().toString());
            }
            try (Writer w = new OutputStreamWriter(new FileOutputStream(shrinkwrap), StandardCharsets.UTF_8)) {
                w.append(new Yaml().dump(installed));
            } catch (IOException e) {
                throw new ConfiguratorException("failed to write shrinkwrap file", e);
            }

            if (!installations.isEmpty()) {
                try {
                    jenkins.restart();
                } catch (RestartNotSupportedException e) {
                    throw new ConfiguratorException("Can't restart master after plugins installation", e);
                }
            }
        }

        try {
            jenkins.save();
        } catch (IOException e) {
            throw new ConfiguratorException("failed to save Jenkins configuration", e);
        }
        return jenkins.getPluginManager();
    }

    /**
     * Collect required plugins as {@link UpdateSite.Plugin} so we can trigger installation if required
     */
    private List<UpdateSite.Plugin> getRequiredPlugins(Map<String, String> plugins, Jenkins jenkins, UpdateCenter updateCenter) {
        List<UpdateSite.Plugin> installations = new ArrayList<>();
        if (plugins != null) {
            for (Map.Entry<String, String> e : plugins.entrySet()) {
                String shortname = e.getKey();
                String version = e.getValue();
                final Plugin plugin = jenkins.getPlugin(shortname);
                if (plugin == null || !plugin.getWrapper().getVersion().equals(e.getValue())) { // Need to install

                    boolean found = false;
                    for (UpdateSite updateSite : updateCenter.getSites()) {
                        // FIXME see https://github.com/jenkins-infra/update-center2/pull/192
                        // get plugin metadata for this specific version of the target plugin
                        final URI metadata = URI.create(updateSite.getUrl()).resolve("download/plugins/" + shortname + "/" + version + "/metadata.json");
                        try (InputStream open = ProxyConfiguration.open(metadata.toURL()).getInputStream()) {
                            final JSONObject json = JSONObject.fromObject(IOUtils.toString(open));
                            final UpdateSite.Plugin installable = updateSite.new Plugin(updateSite.getId(), json);
                            installations.add(installable);
                        } catch (IOException io) {
                            // Not published by this update site
                            System.err.println(io);
                        }
                        found = true;
                        break;
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
        attr.add(new Attribute("updateSites", UpdateSite.class).multiple(true));
        attr.add(new Attribute("required", Plugins.class).multiple(true));
        return attr;
    }
}