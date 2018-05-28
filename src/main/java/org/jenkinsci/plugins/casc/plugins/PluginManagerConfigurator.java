package org.jenkinsci.plugins.casc.plugins;

import hudson.Extension;
import hudson.Plugin;
import hudson.PluginManager;
import hudson.PluginWrapper;
import hudson.ProxyConfiguration;
import hudson.lifecycle.Lifecycle;
import hudson.model.DownloadService;
import hudson.model.UpdateCenter;
import hudson.model.UpdateSite;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.casc.Attribute;
import org.jenkinsci.plugins.casc.BaseConfigurator;
import org.jenkinsci.plugins.casc.Configurator;
import org.jenkinsci.plugins.casc.ConfiguratorException;
import org.jenkinsci.plugins.casc.RootElementConfigurator;
import org.jenkinsci.plugins.casc.model.CNode;
import org.jenkinsci.plugins.casc.model.Mapping;

import javax.annotation.CheckForNull;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension(ordinal = 999)
public class PluginManagerConfigurator extends BaseConfigurator<PluginManager> implements RootElementConfigurator<PluginManager> {

    private final static Logger logger = Logger.getLogger(PluginManagerConfigurator.class.getName());

    @Override
    public Class<PluginManager> getTarget() {
        return PluginManager.class;
    }

    @Override
    public PluginManager getTargetComponent() {
        return Jenkins.getInstance().getPluginManager();
    }

    @Override
    public PluginManager configure(CNode config) throws ConfiguratorException {
        Mapping map = config.asMapping();
        final Jenkins jenkins = Jenkins.getInstance();

        final CNode proxy = map.get("proxy");
        if (proxy != null) {
            Configurator<ProxyConfiguration> pc = Configurator.lookup(ProxyConfiguration.class);
            if (pc == null) throw new ConfiguratorException("ProxyConfiguration not well registered");
            ProxyConfiguration pcc = pc.configure(proxy);
            jenkins.proxy = pcc;
        }

        final CNode sites = map.get("updateSites");
        final UpdateCenter updateCenter = jenkins.getUpdateCenter();
        if (sites != null) {
            Configurator<UpdateSite> usc = Configurator.lookup(UpdateSite.class);
            List<UpdateSite> updateSites = new ArrayList<>();
            for (CNode data : sites.asSequence()) {
                UpdateSite in = usc.configure(data);
                if (in.isDue()) {
                    in.updateDirectly(DownloadService.signatureCheck);
                }
                updateSites.add(in);
            }

            try {
                updateCenter.getSites().replaceBy(updateSites);
            } catch (IOException e) {
                throw new ConfiguratorException("failed to reconfigure updateCenter.sites", e);
            }
        }


        Deque<PluginToInstall> plugins = new LinkedList<>();
        final CNode required = map.get("required");
        if (required != null) {
            for (Map.Entry<String, CNode> entry : required.asMapping().entrySet()) {
                plugins.add(new PluginToInstall(entry.getKey(), entry.getValue().asScalar().getValue()));
            }
        }

        File shrinkwrap = new File("./plugins.txt");
        if (shrinkwrap.exists()) {
            try {
                final List<String> lines = FileUtils.readLines(shrinkwrap, UTF_8);
                for (String line : lines) {
                    int i = line.indexOf(':');
                    plugins.add(new PluginToInstall(line.substring(0,i), line.substring(i+1)));
                }
            } catch (IOException e) {
                throw new ConfiguratorException("failed to load plugins.txt shrinkwrap file", e);
            }
        }

        final PluginManager pluginManager = getTargetComponent();
        if (!plugins.isEmpty()) {

            boolean requireRestart = false;

            UUID correlationId = UUID.randomUUID();

            Set<String> installed = new HashSet<>();

            // Install a plugin from the plugins list.
            // For each installed plugin, get the dependency list and update the plugins list accordingly
            install: while(!plugins.isEmpty()) {
            PluginToInstall p = plugins.pop();
            if (installed.contains(p.shortname)) continue;

            final Plugin plugin = jenkins.getPlugin(p.shortname);
            if (plugin == null || !plugin.getWrapper().getVersion().equals(p.version)) { // Need to install

                // FIXME update sites don't give us metadata about hosted plugins but "latest"
                // So we need to assume the URL layout to bake download metadata
                boolean downloaded = false;
                for (UpdateSite updateSite : updateCenter.getSites()) {
                    JSONObject json = new JSONObject();
                    json.accumulate("name", p.shortname);
                    json.accumulate("version", p.version);
                    json.accumulate("url", "download/plugins/"+p.shortname+"/"+p.version+"/"+p.shortname+".hpi");
                    json.accumulate("dependencies", new JSONArray());
                    final UpdateSite.Plugin installable = updateSite.new Plugin(updateSite.getId(), json);
                    try {
                        final UpdateCenter.UpdateCenterJob job = installable.deploy(true, correlationId).get();
                        if (job.getError() != null) {
                            if (job.getError() instanceof UpdateCenter.DownloadJob.SuccessButRequiresRestart) {
                                requireRestart = true;
                            } else {
                                throw job.getError();
                            }
                        }
                        installed.add(p.shortname);

                        final File jpi = new File(pluginManager.rootDir, p.shortname + ".jpi");
                        try (JarFile jar = new JarFile(jpi)) {
                            String dependencySpec = jar.getManifest().getMainAttributes().getValue("Plugin-Dependencies");
                            if (dependencySpec != null) {
                                plugins.addAll(Arrays.stream(dependencySpec.split(","))
                                        .filter(t -> !t.endsWith(";resolution:=optional"))
                                        .map(t -> t.substring(0, t.indexOf(':')))
                                        .map(a -> new PluginToInstall(a, "latest"))
                                        .collect(Collectors.toList()));
                            }
                        }
                        downloaded = true;
                        break install;
                    } catch (InterruptedException | ExecutionException ex) {
                        logger.info("Failed to download plugin "+p.shortname+':'+p.version+ "from update site "+updateSite.getId());
                    } catch (Throwable ex) {
                        throw new ConfiguratorException("Failed to download plugin "+p.shortname+':'+p.version, ex);
                    }
                }

                if (!downloaded) {
                    throw new ConfiguratorException("Failed to install plugin "+p.shortname+':'+p.version);
                }
            }
        }

        try (PrintWriter w = new PrintWriter(shrinkwrap, UTF_8.name())) {
            for (PluginWrapper pw : pluginManager.getPlugins()) {
                if (pw.getShortName().equals("configuration-as-code")) continue;
                w.println(pw.getShortName() + ":" + pw.getVersionNumber().toString());
            }
        } catch (IOException e) {
            throw new ConfiguratorException("failed to write plugins.txt shrinkwrap file", e);
        }

        if (requireRestart) {
            try {
                    Lifecycle.get().restart();
                } catch (InterruptedException | IOException e) {
                    throw new ConfiguratorException("Can't restart master after plugins installation", e);
                }
            }
        }

        try {
            jenkins.save();
        } catch (IOException e) {
            throw new ConfiguratorException("failed to save Jenkins configuration", e);
        }
        return pluginManager;
    }

    /**
     * Collect required plugins as {@link UpdateSite.Plugin} so we can trigger installation if required
     */
    private List<UpdateSite.Plugin> getRequiredPlugins(Map<String, String> plugins, Jenkins jenkins, UpdateCenter updateCenter) throws ConfiguratorException {
        List<UpdateSite.Plugin> installations = new ArrayList<>();
        if (plugins != null) {
            for (Map.Entry<String, String> e : plugins.entrySet()) {
                String shortname = e.getKey();
                String version = e.getValue();
                final Plugin plugin = jenkins.getPlugin(shortname);
                if (plugin == null || !plugin.getWrapper().getVersion().equals(e.getValue())) { // Need to install

                    boolean found = false;
                    for (UpdateSite updateSite : updateCenter.getSites()) {

                        final UpdateSite.Plugin latest = updateSite.getPlugin(shortname);


                        // FIXME see https://github.com/jenkins-infra/update-center2/pull/192
                        // get plugin metadata for this specific version of the target plugin
                        final URI metadata = URI.create(updateSite.getUrl()).resolve("download/plugins/" + shortname + "/" + version + "/metadata.json");
                        try (InputStream open = ProxyConfiguration.open(metadata.toURL()).getInputStream()) {
                            final JSONObject json = JSONObject.fromObject(IOUtils.toString(open));
                            final UpdateSite.Plugin installable = updateSite.new Plugin(updateSite.getId(), json);
                            installations.add(installable);
                            found = true;
                            break;
                        } catch (IOException io) {
                            // Not published by this update site
                        }
                    }

                    if (!found) {
                        throw new ConfiguratorException("Can't find plugin "+shortname+" in configured update sites");
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

    @CheckForNull
    @Override
    public CNode describe(PluginManager instance) {
        // FIXME
        return null;
    }

    private static class PluginToInstall {
        String shortname;
        String version;

        public PluginToInstall(String shortname, String version) {
            this.shortname = shortname;
            this.version = version;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PluginToInstall that = (PluginToInstall) o;
            return Objects.equals(shortname, that.shortname);
        }

        @Override
        public int hashCode() {

            return Objects.hash(shortname);
        }
    }
}