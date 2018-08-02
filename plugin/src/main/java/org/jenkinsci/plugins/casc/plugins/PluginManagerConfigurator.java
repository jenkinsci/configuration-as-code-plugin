package org.jenkinsci.plugins.casc.plugins;

import hudson.Extension;
import hudson.Plugin;
import hudson.PluginManager;
import hudson.PluginWrapper;
import hudson.ProxyConfiguration;
import hudson.lifecycle.RestartNotSupportedException;
import hudson.model.DownloadService;
import hudson.model.UpdateCenter;
import hudson.model.UpdateSite;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.casc.Attribute;
import org.jenkinsci.plugins.casc.BaseConfigurator;
import org.jenkinsci.plugins.casc.ConfigurationContext;
import org.jenkinsci.plugins.casc.Configurator;
import org.jenkinsci.plugins.casc.ConfiguratorException;
import org.jenkinsci.plugins.casc.RootElementConfigurator;
import org.jenkinsci.plugins.casc.impl.attributes.MultivaluedAttribute;
import org.jenkinsci.plugins.casc.model.CNode;
import org.jenkinsci.plugins.casc.model.Mapping;
import org.jenkinsci.plugins.casc.model.Sequence;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import javax.annotation.CheckForNull;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
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
import java.util.jar.JarFile;
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
    public PluginManager configure(CNode config, ConfigurationContext context) throws ConfiguratorException {
        Mapping map = config.asMapping();
        final Jenkins jenkins = Jenkins.getInstance();

        configureProxy(map, jenkins, context);

        final UpdateCenter updateCenter = configureUpdateSites(map, jenkins, context);

        final PluginManager pluginManager = configurePlugins(map, jenkins, updateCenter, context);

        try {
            jenkins.save();
        } catch (IOException e) {
            throw new ConfiguratorException("failed to save Jenkins configuration", e);
        }
        return pluginManager;
    }

    private void configureProxy(Mapping map, Jenkins jenkins, ConfigurationContext context) throws ConfiguratorException {
        final CNode proxy = map.get("proxy");
        if (proxy != null) {
            Configurator<ProxyConfiguration> pc = Configurator.lookup(ProxyConfiguration.class);
            if (pc == null) throw new ConfiguratorException("ProxyConfiguration not well registered");
            ProxyConfiguration pcc = pc.configure(proxy, context);
            jenkins.proxy = pcc;
        }
    }

    private UpdateCenter configureUpdateSites(Mapping map, Jenkins jenkins, ConfigurationContext context) throws ConfiguratorException {
        final CNode sites = map.get("sites");
        final UpdateCenter updateCenter = jenkins.getUpdateCenter();
        if (sites != null) {
            Configurator<UpdateSite> usc = Configurator.lookup(UpdateSite.class);
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
                logger.fine("Preparing to install "+ p.shortname);
                if (installed.contains(p.shortname)) {
                    logger.fine("Plugin "+p.shortname +" is already installed. Skipping");
                    continue;
                }

                final Plugin plugin = jenkins.getPlugin(p.shortname);
                if (plugin == null || !plugin.getWrapper().getVersion().equals(p.version)) { // Need to install

                    // if plugin is being _upgraded_, not just installed, we NEED to restart
                    requireRestart |= (plugin != null);

                    boolean downloaded = false;
                    final UpdateSite.Plugin installable = getPluginMetadata(updateCenter, p);
                    try {
                        logger.fine("Installing plugin: "+p.shortname);
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
                                pti.forEach( s -> logger.finest("Installing dependant plugin: "+s));
                                logger.finest("Adding "+pti.size()+" plugin(s) to install queue.");
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
            updateSite = updateCenter.getSite(UpdateCenter.PREDEFINED_UPDATE_SITE_ID);
            if (updateSite == null)
                throw new ConfiguratorException("Can't install " + p + ": no default update site");

        } else {
            updateSite = updateCenter.getSite(p.site);
            if (updateSite == null)
                throw new ConfiguratorException("Can't install " + p + ": unknown update site " + p.site);

            final UpdateSite.Plugin hostedPlugin = updateSite.getPlugin(p.shortname);
            if (hostedPlugin == null)
                throw new ConfiguratorException("Can't install " + p + ": " + p.site + " does not host requested plugin");

            if ("latest".equals(p.version)) {
                url = hostedPlugin.url;
            } else {
                final int i = hostedPlugin.url.lastIndexOf(hostedPlugin.version);
                url = hostedPlugin.url.substring(0, i)
                        + p.version
                        + hostedPlugin.url.substring(i + hostedPlugin.version.length());
            }
        }

        JSONObject json = new JSONObject();
        json.accumulate("name", p.shortname);
        json.accumulate("version", p.version);
        json.accumulate("url", url);
        json.accumulate("dependencies", new JSONArray());

        return updateSite.new Plugin(updateSite.getId(), json);
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
                    shrinkwrapped.put(shortname, new PluginToInstall(shortname, line.substring(i+1)));
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
        logger.fine("Writing shrinkwrap file: "+shrinkwrap);
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
    public Set<Attribute> describe() {
        Set<Attribute> attr =  new HashSet<>();
        attr.add(new Attribute<PluginManager, ProxyConfiguration>("proxy", ProxyConfiguration.class));
        attr.add(new MultivaluedAttribute<PluginManager, UpdateSite>("sites", UpdateSite.class));
        attr.add(new MultivaluedAttribute<PluginManager, Plugins>("required", Plugins.class));
        return attr;
    }

    @CheckForNull
    @Override
    public CNode describe(PluginManager instance) throws Exception {
        final Mapping mapping = new Mapping();
        final Configurator cp = Configurator.lookupOrFail(ProxyConfiguration.class);
        final ProxyConfiguration proxy = Jenkins.getInstance().proxy;
        if (proxy != null) {
            mapping.putIfNotNull("proxy", cp.describe(proxy));
        }
        Sequence seq = new Sequence();
        final Configurator cs = Configurator.lookupOrFail(UpdateSite.class);
        for (UpdateSite site : Jenkins.getInstance().getUpdateCenter().getSiteList()) {
            seq.add(cs.describe(site));
        }
        mapping.putIfNotEmpry("sites", seq);
        return mapping;
    }

}