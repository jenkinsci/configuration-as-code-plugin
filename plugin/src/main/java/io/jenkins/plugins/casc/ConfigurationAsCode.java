package io.jenkins.plugins.casc;

import com.google.common.annotations.VisibleForTesting;
import hudson.Extension;
import hudson.Util;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.remoting.Which;
import io.jenkins.plugins.casc.impl.DefaultConfiguratorRegistry;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import io.jenkins.plugins.casc.model.Scalar;
import io.jenkins.plugins.casc.model.Sequence;
import io.jenkins.plugins.casc.model.Source;
import io.jenkins.plugins.casc.yaml.YamlSource;
import io.jenkins.plugins.casc.yaml.YamlUtils;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.kohsuke.stapler.lang.Klass;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.emitter.Emitter;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.resolver.Resolver;
import org.yaml.snakeyaml.serializer.Serializer;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.logging.Level.WARNING;
import static org.yaml.snakeyaml.DumperOptions.FlowStyle.BLOCK;
import static org.yaml.snakeyaml.DumperOptions.ScalarStyle.DOUBLE_QUOTED;
import static org.yaml.snakeyaml.DumperOptions.ScalarStyle.PLAIN;

/**
 * {@linkplain #configure() Main entry point of the logic}.
 *
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension
@Restricted(NoExternalUse.class)
public class ConfigurationAsCode extends GlobalConfiguration {

    public static final String CASC_JENKINS_CONFIG_PROPERTY = "casc.jenkins.config";
    public static final String CASC_JENKINS_CONFIG_ENV = "CASC_JENKINS_CONFIG";
    public static final String DEFAULT_JENKINS_YAML_PATH = "./jenkins.yaml";
    public static final String YAML_FILES_PATTERN = "glob:**.{yml,yaml,YAML,YML}";

    public static final Logger LOGGER = Logger.getLogger(ConfigurationAsCode.class.getName());

    @Inject
    private DefaultConfiguratorRegistry registry;
    private String configurationPath;

    private long lastTimeLoaded;

    private List<String> sources = Collections.emptyList();

    public ConfigurationAsCode() {
        load();
    }

    public String getConfigurationPath() {
        return configurationPath;
    }

    public void setLastTimeLoaded(long lastTimeLoaded) {
        this.lastTimeLoaded = lastTimeLoaded;
    }

    @DataBoundSetter
    public void setConfigurationPath(String configurationPath) throws ConfiguratorException{
        if(Util.fixEmptyAndTrim(configurationPath) == null) {
            configure();
            LOGGER.log(Level.FINE, "Applying default");

        } else {
            this.configurationPath = configurationPath;
            //TODO: Validate sources form the path.
            sources = Collections.singletonList(configurationPath);
            configureWith(getConfigFromSources(getSources()));
            LOGGER.log(Level.FINE, "Replace configuration with: " + configurationPath);
        }
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        req.bindJSON(this, json);
        save();
        return super.configure(req, json);
    }

    @CheckForNull
    @Override
    public String getDisplayName() {
        return "Configuration as Code";
    }

    public Date getLastTimeLoaded() {
        return new Date(lastTimeLoaded);
    }

    public List<String> getSources() {
        return sources;
    }

    // TODO: Do we need it?
    @RequirePOST
    public void doReload(StaplerRequest request, StaplerResponse response) throws Exception {
        if (!Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        configure();
        response.sendRedirect("");
    }

    private static List<YamlSource> getConfigFromSources(List<String> newSources) throws ConfiguratorException {
        List<YamlSource> ret = new ArrayList<>();

        for (String p : newSources) {
            if (isSupportedURI(p)) {
                ret.add(new YamlSource<>(p, YamlSource.READ_FROM_URL));
            } else {
                ret.addAll(configs(p).stream()
                        .map(s -> new YamlSource<>(s, YamlSource.READ_FROM_PATH))
                        .collect(Collectors.toList()));
            }
        }
        return ret;
    }

    /**
     * Defaults to use a file in the current working directory with the name 'jenkins.yaml'
     *
     * Add the environment variable CASC_JENKINS_CONFIG to override the default. Accepts single file or a directory.
     * If a directory is detected, we scan for all .yml and .yaml files
     *
     * @throws Exception when the file provided cannot be found or parsed
     */
    @Initializer(after = InitMilestone.EXTENSIONS_AUGMENTED, before = InitMilestone.JOB_LOADED)
    public static void init() throws Exception {
        get().configure();
    }

    /**
     * Main entry point to start configuration process.
     * @throws ConfiguratorException Configuration error
     */
    public void configure() throws ConfiguratorException {
        configureWith(getStandardConfigSources());
    }

    private List<YamlSource> getStandardConfigSources() throws ConfiguratorException {
        List<String> standardConfig = getStandardConfig();
        String last = standardConfig.get(standardConfig.size() - 1);
        //TODO: remove this side effect from the getter
        sources = Collections.singletonList(last);
        return getConfigFromSources(standardConfig);
    }

    private List<String> getStandardConfig() {
        List<String> configParameters = getBundledCasCURIs();

        // Prioritization loaded files Load from configuration
        if (configurationPath != null) {
            configParameters.add(configurationPath);
            return configParameters;
        }

        String configParameter = System.getProperty(
                CASC_JENKINS_CONFIG_PROPERTY,
                System.getenv(CASC_JENKINS_CONFIG_ENV)
        );

        if (configParameter == null) {
            if (Files.exists(Paths.get(DEFAULT_JENKINS_YAML_PATH))) {
                configParameter = DEFAULT_JENKINS_YAML_PATH;
            }
        }

        if (configParameters.isEmpty() && configParameter != null) {
            // Add external config parameter
            configParameters.add(configParameter);
        }
        if (configParameters.isEmpty()) {
            LOGGER.log(Level.FINE, "No configuration set nor default config file");
        }
        return configParameters;
    }

    public List<String> getBundledCasCURIs() {
        final String cascFile = "/WEB-INF/" + DEFAULT_JENKINS_YAML_PATH;
        final String cascDirectory = "/WEB-INF/" + DEFAULT_JENKINS_YAML_PATH + ".d/";
        List<String> res = new ArrayList<>();

        final ServletContext servletContext = Jenkins.getInstance().servletContext;
        try {
            URL bundled = servletContext.getResource(cascFile);
            if (bundled != null) {
                res.add(bundled.toString());
            }
        } catch (IOException e) {
            LOGGER.log(WARNING, "Failed to load " + cascFile, e);
        }

        PathMatcher matcher = FileSystems.getDefault().getPathMatcher(YAML_FILES_PATTERN);
        Set<String> resources = servletContext.getResourcePaths(cascDirectory);
        if (resources!=null) {
            // sort to execute them in a deterministic order
            for (String cascItem : new TreeSet<>(resources)) {
                try {
                    URL bundled = servletContext.getResource(cascItem);
                    if (bundled != null && matcher.matches(
                            new File(bundled.getPath()).toPath())) {
                        res.add(bundled.toString());
                    } //TODO: else do some handling?
                } catch (IOException e) {
                    LOGGER.log(WARNING, "Failed to execute " + res, e);
                }
            }
        }

        return res;
    }

    // TODO: Do we still need that?
    @RequirePOST
    public void doCheck(StaplerRequest req, StaplerResponse res) throws Exception {

        if (!Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        final Map<Source, String> issues = checkWith(new YamlSource<HttpServletRequest>(req, YamlSource.READ_FROM_REQUEST));
        res.setContentType("application/json");
        final JSONArray warnings = new JSONArray();
        issues.entrySet().stream().map(e -> new JSONObject().accumulate("line", e.getKey().line).accumulate("warning", e.getValue()))
                .forEach(warnings::add);
        warnings.write(res.getWriter());
    }

    // TODO: Do we still need that?
    @RequirePOST
    public void doApply(StaplerRequest req, StaplerResponse res) throws Exception {

        if (!Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        configureWith(new YamlSource<HttpServletRequest>(req, YamlSource.READ_FROM_REQUEST));
    }

    /**
     * Export live jenkins instance configuration as Yaml
     * @throws Exception
     */
    // TODO: Find a way to process custom buttons from config.jelly
    @RequirePOST
    public void doExport(StaplerRequest req, StaplerResponse res) throws Exception {

        if (!Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        res.setContentType("application/x-yaml; charset=utf-8");
        res.addHeader("Content-Disposition", "attachment; filename=jenkins.yaml");
        export(res.getOutputStream());
    }

    @org.kohsuke.accmod.Restricted(NoExternalUse.class)
    public void export(OutputStream out) throws Exception {

        final List<NodeTuple> tuples = new ArrayList<>();

        final ConfigurationContext context = new ConfigurationContext(registry);
        for (RootElementConfigurator root : RootElementConfigurator.all()) {
            final CNode config = root.describe(root.getTargetComponent(context), context);
            final Node valueNode = toYaml(config);
            if (valueNode == null) continue;
            tuples.add(new NodeTuple(
                    new ScalarNode(Tag.STR, root.getName(), null, null, PLAIN),
                    valueNode));
        }

        MappingNode root = new MappingNode(Tag.MAP, tuples, BLOCK);
        try (Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
            serializeYamlNode(root, writer);
        } catch (IOException e) {
            throw new YAMLException(e);
        }
    }

    @VisibleForTesting
    @org.kohsuke.accmod.Restricted(NoExternalUse.class)
    public static void serializeYamlNode(Node root, Writer writer) throws IOException {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(BLOCK);
        options.setDefaultScalarStyle(PLAIN);
        options.setSplitLines(true);
        options.setPrettyFlow(true);
        Serializer serializer = new Serializer(new Emitter(writer, options), new Resolver(),
                options, null);
        serializer.open();
        serializer.serialize(root);
        serializer.close();
    }

    @CheckForNull
    @VisibleForTesting
    @org.kohsuke.accmod.Restricted(NoExternalUse.class)
    public Node toYaml(CNode config) throws ConfiguratorException {

        if (config == null) return null;

        switch (config.getType()) {
            case MAPPING:
                final Mapping mapping = config.asMapping();
                final List<NodeTuple> tuples = new ArrayList<>();
                final List<Map.Entry<String, CNode>> entries = new ArrayList<>(mapping.entrySet());
                entries.sort(Comparator.comparing(Map.Entry::getKey));
                for (Map.Entry<String, CNode> entry : entries) {
                    final Node valueNode = toYaml(entry.getValue());
                    if (valueNode == null) continue;
                    tuples.add(new NodeTuple(
                            new ScalarNode(Tag.STR, entry.getKey(), null, null, PLAIN),
                            valueNode));

                }
                if (tuples.isEmpty()) return null;

                return new MappingNode(Tag.MAP, tuples, BLOCK);

            case SEQUENCE:
                final Sequence sequence = config.asSequence();
                List<Node> nodes = new ArrayList<>();
                for (CNode cNode : sequence) {
                    final Node valueNode = toYaml(cNode);
                    if (valueNode == null) continue;
                    nodes.add(valueNode);
                }
                if (nodes.isEmpty()) return null;
                return new SequenceNode(Tag.SEQ, nodes, BLOCK);

            case SCALAR:
            default:
                final Scalar scalar = config.asScalar();
                final String value = scalar.getValue();
                if (value == null || value.length() == 0) return null;

                final DumperOptions.ScalarStyle style = scalar.isRaw() ? PLAIN : DOUBLE_QUOTED;

                return new ScalarNode(getTag(scalar.getFormat()), value, null, null, style);
        }
    }

    private Tag getTag(Scalar.Format format) {
        switch (format) {
            case NUMBER:
                return Tag.INT;
            case BOOLEAN:
                return Tag.BOOL;
            case STRING:
            default:
                return Tag.STR;
        }
    }

    public void configure(String ... configParameters) throws ConfiguratorException {
        configure(Arrays.asList(configParameters));
    }

    public void configure(Collection<String> configParameters) throws ConfiguratorException {

        List<YamlSource> configs = new ArrayList<>();

        for (String p : configParameters) {
            if (isSupportedURI(p)) {
                configs.add(new YamlSource<>(p, YamlSource.READ_FROM_URL));
            } else {
                configs.addAll(configs(p).stream()
                        .map(s -> new YamlSource<>(s, YamlSource.READ_FROM_PATH))
                        .collect(Collectors.toList()));
            }
            sources = Collections.singletonList(p);
        }
        configureWith(configs);
        lastTimeLoaded = System.currentTimeMillis();
    }

    public static boolean isSupportedURI(String configurationParameter) {
        if(configurationParameter == null) {
            return false;
        }
        final List<String> supportedProtocols = Arrays.asList("https","http","file");
        URI uri = URI.create(configurationParameter);
        if(uri == null || uri.getScheme() == null) {
            return false;
        }
        return supportedProtocols.contains(uri.getScheme());
    }

    @org.kohsuke.accmod.Restricted(NoExternalUse.class)
    public void configureWith(YamlSource source) throws ConfiguratorException {
        final List<YamlSource> sources = getStandardConfigSources();
        sources.add(source);
        configureWith(sources);
    }

    private void configureWith(List<YamlSource> sources) throws ConfiguratorException {
        final Mapping map = YamlUtils.loadFrom(sources);
        configureWith(map.entrySet());
    }

    @org.kohsuke.accmod.Restricted(NoExternalUse.class)
    public Map<Source, String> checkWith(YamlSource source) throws ConfiguratorException {
        final List<YamlSource> sources = getStandardConfigSources();
        sources.add(source);
        return checkWith(sources);
    }

    private Map<Source, String> checkWith(List<YamlSource> sources) throws ConfiguratorException {
        if (sources.isEmpty()) return null;
        final Mapping map = YamlUtils.loadFrom(sources);
        return checkWith(map.entrySet());
    }

    /**
     * Recursive search for all {@link #YAML_FILES_PATTERN} in provided base path
     *
     * @param path base path to start (can be file or directory)
     * @return list of all paths matching pattern. Only base file itself if it is a file matching pattern
     */
    public static List<Path> configs(String path) throws ConfiguratorException {
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher(YAML_FILES_PATTERN);

        try (Stream<Path> stream = Files.find(Paths.get(path), Integer.MAX_VALUE,
                (next, attrs) -> attrs.isRegularFile() && matcher.matches(next))) {
            return stream.collect(Collectors.toList());
        } catch (NoSuchFileException e) {
            throw new ConfiguratorException("File does not exist: " + path, e);
        } catch (IOException e) {
            throw new IllegalStateException("failed config scan for " + path, e);
        }
    }

    private static Stream<? extends Map.Entry<String, Object>> entries(Reader config) {
        return ((Map<String, Object>) new Yaml().loadAs(config, Map.class)).entrySet().stream();
    }


    @FunctionalInterface
    private interface ConfigratorOperation {

        Object apply(RootElementConfigurator configurator, CNode node) throws ConfiguratorException;
    }

    /**
     * Configuration with help of {@link RootElementConfigurator}s.
     * Corresponding configurator is searched by entry key, passing entry value as object with all required properties.
     *
     * @param entries key-value pairs, where key should match to root configurator and value have all required properties
     * @throws ConfiguratorException configuration error
     */
    private static void invokeWith(Set<Map.Entry<String, CNode>> entries, ConfigratorOperation function) throws ConfiguratorException {

        // Run configurators by order, consuming entries until all have found a matching configurator
        // configurators order is important so that org.jenkinsci.plugins.casc.plugins.PluginManagerConfigurator run
        // before any other, and can install plugins required by other configuration to successfully parse yaml data
        for (RootElementConfigurator configurator : RootElementConfigurator.all()) {
            final Iterator<Map.Entry<String, CNode>> it = entries.iterator();
            while (it.hasNext()) {
                Map.Entry<String, CNode> entry = it.next();
                if (! entry.getKey().equalsIgnoreCase(configurator.getName())) {
                    continue;
                }
                try {
                    function.apply(configurator, entry.getValue());
                    it.remove();
                    break;
                } catch (ConfiguratorException e) {
                    throw new ConfiguratorException(
                            configurator,
                            format("error configuring '%s' with %s configurator", entry.getKey(), configurator.getClass()), e
                    );
                }
            }
        }

        if (!entries.isEmpty()) {
            final Map.Entry<String, CNode> next = entries.iterator().next();
            throw new ConfiguratorException(format("No configurator for root element <%s>", next.getKey()));
        }
    }

    private void configureWith(Set<Map.Entry<String, CNode>> entries) throws ConfiguratorException {
        final ObsoleteConfigurationMonitor monitor = ObsoleteConfigurationMonitor.get();
        monitor.reset();
        ConfigurationContext context = new ConfigurationContext(registry);
        context.addListener(monitor::record);
        invokeWith(entries, (configurator, config) -> configurator.configure(config, context));
    }

    public Map<Source, String> checkWith(Set<Map.Entry<String, CNode>> entries) throws ConfiguratorException {
        Map<Source, String> issues = new HashMap<>();
        ConfigurationContext context = new ConfigurationContext(registry);
        context.addListener( (node,message) -> issues.put(node.getSource(), message) );
        invokeWith(entries, (configurator, config) -> configurator.check(config, context));
        return issues;
    }


    public static ConfigurationAsCode get() {
        return Jenkins.getInstance().getExtensionList(ConfigurationAsCode.class).get(0);
    }

    /**
     * Used for documentation generation in config.jelly
     */
    public Collection<?> getRootConfigurators() {
        return new LinkedHashSet<>(RootElementConfigurator.all());
    }

    /**
     * Used for documentation generation in config.jelly
     */
    public Collection<?> getConfigurators() {
        List<RootElementConfigurator> roots = RootElementConfigurator.all();
        final ConfigurationContext context = new ConfigurationContext(registry);
        Set<Object> elements = new LinkedHashSet<>(roots);
        for (RootElementConfigurator root : roots) {
            listElements(elements, root.describe(), context);
        }
        return elements;
    }

    /**
     * Recursive configurators tree walk (DFS).
     * Collects all configurators starting from root ones in {@link #getConfigurators()}
     *  @param elements   linked set (to save order) of visited elements
     * @param attributes siblings to find associated configurators and dive to next tree levels
     * @param context
     */
    private void listElements(Set<Object> elements, Set<Attribute<?,?>> attributes, ConfigurationContext context) {
        attributes.stream()
                .map(Attribute::getType)
                .map(context::lookup)
                .filter(Objects::nonNull)
                .map(c -> c.getConfigurators(context))
                .flatMap(Collection::stream)
                .forEach(configurator -> {
                    if (elements.add(configurator)) {
                        listElements(elements, ((Configurator)configurator).describe(), context);   // some unexpected type erasure force to cast here
                    }
                });
    }

    // --- UI helper methods

    /**
     * Retrieve the html help tip associated to an attribute, used in documentation.jelly
     * FIXME would prefer &lt;st:include page="help-${a.name}.html" class="${c.target}" optional="true"/&gt;
     * @param attribute to get help for
     * @return String that shows help. May be empty
     * @throws IOException if the resource cannot be read
     */
    @Nonnull
    public String getHtmlHelp(Class type, String attribute) throws IOException {
        final URL resource = Klass.java(type).getResource("help-" + attribute + ".html");
        if (resource != null) {
            return IOUtils.toString(resource.openStream());
        }
        return "";
    }

    /**
     * Retrieve which plugin do provide this extension point, used in documentation.jelly
     *
     * @return String representation of the extension source, usually artifactId.
     */
    @CheckForNull
    public String getExtensionSource(Configurator c) throws IOException {
        final Class e = c.getImplementedAPI();
        final String jar = Which.jarFile(e).getName();
        if (jar.startsWith("jenkins-core-")) { // core jar has version in name
            return "jenkins-core";
        }
        return jar.substring(0, jar.lastIndexOf('.'));
    }
}
