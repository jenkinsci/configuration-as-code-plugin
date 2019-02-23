package io.jenkins.plugins.casc.plugins;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import hudson.Extension;
import hudson.Plugin;
import hudson.PluginManager;
import hudson.PluginWrapper;
import hudson.ProxyConfiguration;
import hudson.lifecycle.RestartNotSupportedException;
import hudson.model.DownloadService;
import hudson.model.UpdateCenter;
import hudson.model.UpdateSite;
import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.BaseConfigurator;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.Configurator;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.RootElementConfigurator;
import io.jenkins.plugins.casc.impl.attributes.MultivaluedAttribute;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import io.jenkins.plugins.casc.model.Sequence;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import javax.annotation.CheckForNull;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension(ordinal = 999)
@Restricted(NoExternalUse.class)
public class PluginManagerConfigurator extends BaseConfigurator<PluginManager> implements RootElementConfigurator<PluginManager> {

    private final static Logger logger = Logger.getLogger(PluginManagerConfigurator.class.getName());

    @Override
    public Class<PluginManager> getTarget() {
        return PluginManager.class;
    }

    @Override
    public PluginManager getTargetComponent(ConfigurationContext context) {
        return Jenkins.getInstance().getPluginManager();
    }

    @Override
    protected PluginManager instance(Mapping mapping, ConfigurationContext context) {
        return getTargetComponent(context);
    }

    @Override
    protected void configure(Mapping config, PluginManager instance, boolean dryrun, ConfigurationContext context) throws ConfiguratorException {

        // PluginManager has no dry-run mode : we need to actually install plugins, or we just can't check
        // the other elements of the configuration regarding required plugins.

        Mapping map = config.asMapping();
        final Jenkins jenkins = Jenkins.getInstance();

        configureProxy(map, jenkins, context);

        final UpdateCenter updateCenter = configureUpdateSites(map, jenkins, context);

        configurePlugins(map, jenkins, updateCenter, context);

        try {
            jenkins.save();
        } catch (IOException e) {
            throw new ConfiguratorException("failed to save Jenkins configuration", e);
        }
    }

    private void configureProxy(Mapping map, Jenkins jenkins, ConfigurationContext context) throws ConfiguratorException {
        final CNode proxy = map.get("proxy");
        if (proxy != null) {
            Configurator<ProxyConfiguration> pc = context.lookup(ProxyConfiguration.class);
            if (pc == null) throw new ConfiguratorException("ProxyConfiguration not well registered");
            ProxyConfiguration pcc = pc.configure(proxy, context);
            jenkins.proxy = pcc;
        }
    }

    private UpdateCenter configureUpdateSites(Mapping map, Jenkins jenkins, ConfigurationContext context) throws ConfiguratorException {
        final CNode sites = map.get("sites");
        final UpdateCenter updateCenter = jenkins.getUpdateCenter();
        if (sites != null) {
            Configurator<UpdateSite> usc = context.lookup(UpdateSite.class);
            List<UpdateSite> updateSites = new ArrayList<>();
            for (CNode data : sites.asSequence()) {
                UpdateSite in = usc.configure(data, context);
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
        return updateCenter;
    }

    private PluginManager configurePlugins(Mapping map, Jenkins jenkins, UpdateCenter updateCenter, ConfigurationContext context) throws ConfiguratorException {
        Queue<PluginToInstall> plugins = new LinkedList<>();
        final CNode required = map.get("required");
        if (required != null) {
            for (Map.Entry<String, CNode> entry : required.asMapping().entrySet()) {
                plugins.add(new PluginToInstall(entry.getKey(), entry.getValue().asScalar().getValue()));
            }
        }

        File shrinkwrap = readShrinkwrapFile(jenkins, plugins);


        final PluginManager pluginManager = getTargetComponent(context);
        if (!plugins.isEmpty()) {
            logger.log(java.util.logging.Level.CONFIG, String.format("Using plugin root dir: '%s'", pluginManager.rootDir));
            boolean requireRestart = false;
            Set<String> installed = new HashSet<>();

            // Install a plugin from the plugins list.
            // For each installed plugin, get the dependency list and update the plugins list accordingly
            install:
            while (!plugins.isEmpty()) {
                PluginToInstall p = plugins.remove();
                logger.fine("Preparing to install " + p.shortname);
                if (installed.contains(p.shortname)) {
                    logger.fine("Plugin " + p.shortname + " is already installed. Skipping");
                    continue;
                }

                final Plugin plugin = jenkins.getPlugin(p.shortname);
                if (plugin != null) {
                    // Plugin is already installed, lets check we run the expected version

                    if (!Character.isDigit(p.version.charAt(0)) && !"latest".equals(p.version)) {
                        // explicit download URL, we need to assume we run the expected one as we have no way
                        // to guess where the installed version has been downloaded from
                        continue;
                    }
                    if (plugin.getWrapper().getVersion().equals(p.version)) {
                        // We are already running the required version
                        continue;
                    }
                }

                final UpdateSite.Plugin installable = getPluginMetadata(updateCenter, p);
                if ("latest".equals(p.version) && plugin != null && plugin.getWrapper().getVersion().equals(installable.version)) {
                    // installed version is already latest version available in update center
                    continue;
                }

                // if we update an installed plugin, Jenkins has to be restarted
                requireRestart |= (plugin != null);


                boolean downloaded = false;
                try {
                    logger.fine("Installing plugin: " + p.shortname);
                    final UpdateCenter.UpdateCenterJob job = installable.deploy(false).get();
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
                            List<PluginToInstall> pti = Arrays.stream(dependencySpec.split(","))
                                    .filter(t -> !t.endsWith(";resolution:=optional"))
                                    .map(t -> t.substring(0, t.indexOf(':')))
                                    .map(a -> new PluginToInstall(a, "latest"))
                                    .collect(Collectors.toList());
                            pti.forEach(s -> logger.finest("Installing dependant plugin: " + s));
                            logger.finest("Adding " + pti.size() + " plugin(s) to install queue.");
                            plugins.addAll(pti);
                        }
                    }
                    downloaded = true;
                    continue install;
                } catch (InterruptedException | ExecutionException ex) {
                    logger.info("Failed to download plugin " + p.shortname + ':' + p.version + " from " + p.site);
                } catch (Throwable ex) {
                    throw new ConfiguratorException("Failed to download plugin " + p.shortname + ':' + p.version, ex);
                }

                if (!downloaded) {
                    throw new ConfiguratorException("Failed to install plugin " + p.shortname + ':' + p.version);
                }
                logger.fine("Done installing plugins");
            }
            writeShrinkwrapFile(jenkins, shrinkwrap, pluginManager);

            if (requireRestart) {
                try {
                    jenkins.restart();
                } catch (RestartNotSupportedException e) {
                    throw new ConfiguratorException("Can't restart master after plugins installation", e);
                }
            }
        }
        return pluginManager;
    }

    /**
     * Resolve specific plugin and version metadata and download URL.
     * Update sites doesn't give us metadata about hosted plugins, but "latest" version. So here we workaround this by
     * searching update site for plugin shortname and we bake specific version's URL based on this URL, assuming this
     * will be the same but version part in the path. This is a bit fragile but we don't have a better way so far.
     * see <a href="https://issues.jenkins-ci.org/browse/INFRA-1696">INFRA-1696</a> for status
     *
     * @param updateCenter
     * @param p
     * @return
     * @throws ConfiguratorException
     */
    private UpdateSite.Plugin getPluginMetadata(UpdateCenter updateCenter, PluginToInstall p) throws ConfiguratorException {
        String url;
        UpdateSite updateSite;
        if (!"latest".equals(p.version) && !Character.isDigit(p.version.charAt(0))) {
            // This is not a version number
            // We also support plain download URL for custom plugins
            url = p.version;
            // we don't really care about update site, but we need one to create a Plugin instance
            updateSite = updateCenter.getSites().get(0);
            if (updateSite == null)
                throw new ConfiguratorException("Can't install " + p + ": no update site declared");

            JSONObject json = new JSONObject();
            json.accumulate("name", p.shortname);
            json.accumulate("version", p.version);
            json.accumulate("url", url);
            json.accumulate("dependencies", new JSONArray());

            return updateSite.new Plugin(updateSite.getId(), json);
        } else {
            updateSite = updateCenter.getSite(p.site);
            if (updateSite == null)
                throw new ConfiguratorException("Can't install " + p + ": unknown update site " + p.site);

            if ("latest".equals(p.version)) {
                final UpdateSite.Plugin plugin = updateSite.getPlugin(p.shortname);
                if (plugin == null) {
                    throw new ConfiguratorException("update site " + p.site + " isn't hosting plugin " + p.shortname);
                }
                return plugin;
            }

            final JSONObject versions = getPluginVersions(updateSite);
            if (versions == null) {
                throw new ConfiguratorException("update site " + p.site + " doesn't host plugin-versions.json metadata. Use plain download URL as 'version'");
            }

            final JSONObject plugin = versions.getJSONObject(p.shortname);
            if (plugin == null) {
                throw new ConfiguratorException("update site " + p.site + " isn't hosting plugin " + p.shortname);
            }
            final JSONObject version = plugin.getJSONObject(p.version);
            if (version == null) {
                throw new ConfiguratorException("update site " + p.site + " isn't hosting plugin " + p.shortname + " version " + p.version);
            }
            return updateSite.new Plugin(updateSite.getId(), version);
        }

    }

    Cache<String, JSONObject> pluginVersions = CacheBuilder.newBuilder().build();

    private JSONObject getPluginVersions(UpdateSite updateSite) {

        // TODO use jenkins-core API for this once UpdateSite offers.
        try {
            return pluginVersions.get(updateSite.getId(), () -> {
                final File file = new File(Jenkins.getInstance().getRootDir(),
                        "updates/" + updateSite.getId() + "plugin-versions.json");

                boolean needUpdate = !file.exists() || System.currentTimeMillis() - file.lastModified() > TimeUnit.DAYS.toMillis(1);
                if (needUpdate) {

                    final URI metadata = new URI(updateSite.getUrl()).resolve("./plugin-versions.json");
                    try (InputStream is = ProxyConfiguration.open(metadata.toURL()).getInputStream()) {
                        FileUtils.copyInputStreamToFile(is, file);
                    } catch (IOException e) {
                        logger.log(Level.WARNING, "Failed to download plugin-versions metadata from " + updateSite.getUrl(), e);
                    }
                }

                try {
                    return JSONObject.fromObject(FileUtils.readFileToString(file)).getJSONObject("plugins");
                } catch (IOException e) {
                    return null;
                }
            });
        } catch (ExecutionException e) {
            return null;
        }
    }

    private File readShrinkwrapFile(Jenkins jenkins, Queue<PluginToInstall> plugins) throws ConfiguratorException {
        File shrinkwrap = new File(jenkins.getRootDir(), "plugins.txt");
        logger.log(java.util.logging.Level.CONFIG, String.format("Using shrinkwrap file: '%s'", shrinkwrap.getAbsoluteFile()));

        Map<String, PluginToInstall> shrinkwrapped = new HashMap<>();
        if (shrinkwrap.exists()) {
            try {
                final List<String> lines = FileUtils.readLines(shrinkwrap, UTF_8);
                for (String line : lines) {
                    int i = line.indexOf(':');
                    final String shortname = line.substring(0, i);
                    shrinkwrapped.put(shortname, new PluginToInstall(shortname, line.substring(i + 1)));
                }
            } catch (IOException e) {
                throw new ConfiguratorException("failed to load plugins.txt shrinkwrap file", e);
            }

            // Check if required plugin list has been updated, in which case the shrinkwrap file is obsolete
            boolean outdated = false;
            for (PluginToInstall plugin : plugins) {
                final PluginToInstall other = shrinkwrapped.get(plugin.shortname);
                if (other == null || !other.equals(plugin)) {
                    // plugins was added or version updates, so shrinkwrap isn't relevant anymore
                    outdated = true;
                    break;
                }
            }
            if (!outdated) plugins.addAll(shrinkwrapped.values());
        }
        return shrinkwrap;
    }

    private void writeShrinkwrapFile(Jenkins jenkins, File shrinkwrap, PluginManager pluginManager) throws ConfiguratorException {
        logger.fine("Writing shrinkwrap file: " + shrinkwrap);
        try (PrintWriter w = new PrintWriter(shrinkwrap, UTF_8.name())) {
            for (PluginWrapper pw : pluginManager.getPlugins()) {
                if (pw.getShortName().equals("configuration-as-code")) continue;
                String from = UpdateCenter.PREDEFINED_UPDATE_SITE_ID;
                for (UpdateSite site : jenkins.getUpdateCenter().getSites()) {
                    if (site.getPlugin(pw.getShortName()) != null) {
                        from = site.getId();
                        break;
                    }
                }
                w.println(pw.getShortName() + ':' + pw.getVersionNumber().toString() + '@' + from);
            }
        } catch (IOException e) {
            throw new ConfiguratorException("failed to write plugins.txt shrinkwrap file", e);
        }
    }

    @Override
    public String getName() {
        return "plugins";
    }

    @Override
    public Set<Attribute<PluginManager, ?>> describe() {
        Set<Attribute<PluginManager, ?>> attr = new HashSet<>();
        attr.add(new Attribute<PluginManager, ProxyConfiguration>("proxy", ProxyConfiguration.class));
        attr.add(new MultivaluedAttribute<PluginManager, UpdateSite>("sites", UpdateSite.class));
        attr.add(new MultivaluedAttribute<PluginManager, Plugins>("required", Plugins.class));
        return attr;
    }

    @CheckForNull
    @Override
    public CNode describe(PluginManager instance, ConfigurationContext context) throws Exception {
        final Mapping mapping = new Mapping();
        final Configurator cp = context.lookupOrFail(ProxyConfiguration.class);
        final ProxyConfiguration proxy = Jenkins.getInstance().proxy;
        if (proxy != null) {
            mapping.putIfNotNull("proxy", cp.describe(proxy, context));
        }
        Sequence seq = new Sequence();
        final Configurator cs = context.lookupOrFail(UpdateSite.class);
        for (UpdateSite site : Jenkins.getInstance().getUpdateCenter().getSiteList()) {
            seq.add(cs.describe(site, context));
        }
        mapping.putIfNotEmpry("sites", seq);
        return mapping;
    }

}
