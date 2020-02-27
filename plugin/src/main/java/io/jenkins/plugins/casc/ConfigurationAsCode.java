package io.jenkins.plugins.casc;

import com.google.common.annotations.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Functions;
import hudson.PluginManager;
import hudson.Util;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.ManagementLink;
import hudson.remoting.Which;
import hudson.security.ACL;
import hudson.security.ACLContext;
import hudson.util.FormValidation;
import io.jenkins.plugins.casc.impl.DefaultConfiguratorRegistry;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import io.jenkins.plugins.casc.model.Scalar;
import io.jenkins.plugins.casc.model.Scalar.Format;
import io.jenkins.plugins.casc.model.Sequence;
import io.jenkins.plugins.casc.model.Source;
import io.jenkins.plugins.casc.snakeyaml.DumperOptions;
import io.jenkins.plugins.casc.snakeyaml.emitter.Emitter;
import io.jenkins.plugins.casc.snakeyaml.error.YAMLException;
import io.jenkins.plugins.casc.snakeyaml.nodes.MappingNode;
import io.jenkins.plugins.casc.snakeyaml.nodes.Node;
import io.jenkins.plugins.casc.snakeyaml.nodes.NodeTuple;
import io.jenkins.plugins.casc.snakeyaml.nodes.ScalarNode;
import io.jenkins.plugins.casc.snakeyaml.nodes.SequenceNode;
import io.jenkins.plugins.casc.snakeyaml.nodes.Tag;
import io.jenkins.plugins.casc.snakeyaml.resolver.Resolver;
import io.jenkins.plugins.casc.snakeyaml.serializer.Serializer;
import io.jenkins.plugins.casc.yaml.YamlSource;
import io.jenkins.plugins.casc.yaml.YamlUtils;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
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
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.kohsuke.stapler.lang.Klass;
import org.kohsuke.stapler.verb.POST;

import static io.jenkins.plugins.casc.SchemaGeneration.writeJSONSchema;
import static io.jenkins.plugins.casc.snakeyaml.DumperOptions.FlowStyle.BLOCK;
import static io.jenkins.plugins.casc.snakeyaml.DumperOptions.ScalarStyle.DOUBLE_QUOTED;
import static io.jenkins.plugins.casc.snakeyaml.DumperOptions.ScalarStyle.LITERAL;
import static io.jenkins.plugins.casc.snakeyaml.DumperOptions.ScalarStyle.PLAIN;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

/**
 * {@linkplain #configure() Main entry point of the logic}.
 *
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension
public class ConfigurationAsCode extends ManagementLink {

    public static final String CASC_JENKINS_CONFIG_PROPERTY = "casc.jenkins.config";
    public static final String CASC_JENKINS_CONFIG_ENV = "CASC_JENKINS_CONFIG";
    public static final String DEFAULT_JENKINS_YAML_PATH = "jenkins.yaml";
    public static final String YAML_FILES_PATTERN = "glob:**.{yml,yaml,YAML,YML}";

    private static final Logger LOGGER = Logger.getLogger(ConfigurationAsCode.class.getName());

    @Inject
    private DefaultConfiguratorRegistry registry;

    private long lastTimeLoaded;

    private List<String> sources = Collections.emptyList();

    @CheckForNull
    @Override
    public String getIconFileName() {
        return "/plugin/configuration-as-code/img/logo-head.svg";
    }

    @CheckForNull
    @Override
    public String getDisplayName() {
        return "Configuration as Code";
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return "configuration-as-code";
    }

    @Override
    public String getDescription() {
        return "Reload your configuration or update configuration source";
    }

    public Date getLastTimeLoaded() {
        return new Date(lastTimeLoaded);
    }

    public List<String> getSources() {
        return sources;
    }

    @RequirePOST
    @Restricted(NoExternalUse.class)
    public void doReload(StaplerRequest request, StaplerResponse response) throws Exception {
        if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        configure();
        response.sendRedirect("");
    }

    @RequirePOST
    @Restricted(NoExternalUse.class)
    public void doReplace(StaplerRequest request, StaplerResponse response) throws Exception {
        if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        String newSource = request.getParameter("_.newSource");
        String normalizedSource = Util.fixEmptyAndTrim(newSource);
        File file = new File(Util.fixNull(normalizedSource));
        if (file.exists() || ConfigurationAsCode.isSupportedURI(normalizedSource)) {
            List<String> candidatePaths = Collections.singletonList(normalizedSource);
            List<YamlSource> candidates = getConfigFromSources(candidatePaths);
            if (canApplyFrom(candidates)) {
                sources = candidatePaths;
                configureWith(getConfigFromSources(getSources()));
                CasCGlobalConfig config = GlobalConfiguration.all().get(CasCGlobalConfig.class);
                if (config != null) {
                    config.setConfigurationPath(normalizedSource);
                    config.save();
                }
                LOGGER.log(Level.FINE, "Replace configuration with: " + normalizedSource);
            } else {
                LOGGER.log(Level.WARNING, "Provided sources could not be applied");
                // todo: show message in UI
            }
        } else {
            LOGGER.log(Level.FINE, "No such source exists, applying default");
            // May be do nothing instead?
            configure();
        }
        response.sendRedirect("");
    }

    private boolean canApplyFrom(List<YamlSource> yamlSources) {
        try {
            checkWith(yamlSources);
            return true;
        } catch (ConfiguratorException e) {
            // ignore and return false
        }
        return false;
    }

    @POST
    @Restricted(NoExternalUse.class)
    public FormValidation doCheckNewSource(@QueryParameter String newSource) {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        String normalizedSource = Util.fixEmptyAndTrim(newSource);
        File file = new File(Util.fixNull(normalizedSource));
        if (normalizedSource == null) {
            return FormValidation.ok(); // empty, do nothing
        }
        if (!file.exists() && !ConfigurationAsCode.isSupportedURI(normalizedSource)) {
            return FormValidation.error("Configuration cannot be applied. File or URL cannot be parsed or do not exist.");
        }

        List<YamlSource> yamlSources = Collections.emptyList();
        try {
            List<String> sources = Collections.singletonList(normalizedSource);
            yamlSources = getConfigFromSources(sources);
            final Map<Source, String> issues = checkWith(yamlSources);
            final JSONArray errors = collectProblems(issues, "error");
            if (!errors.isEmpty()) {
                return FormValidation.error(errors.toString());
            }
            final JSONArray warnings = collectProblems(issues, "warning");
            if (!warnings.isEmpty()) {
                return FormValidation.warning(warnings.toString());
            }
            return FormValidation.okWithMarkup("The configuration can be applied");
        } catch (ConfiguratorException | IllegalArgumentException e) {
            return FormValidation.error(e, e.getCause() == null ? e.getMessage() : e.getCause().getMessage());
        } finally {
            closeSources(yamlSources);
        }
    }

    private JSONArray collectProblems(Map<Source, String> issues, String severity) {
        final JSONArray problems = new JSONArray();
        issues.entrySet().stream().map(e -> new JSONObject().accumulate("line", e.getKey().line).accumulate(severity, e.getValue()))
                .forEach(problems::add);
        return problems;
    }

    private void appendSources(List<YamlSource> sources, String source) throws ConfiguratorException {
        if (isSupportedURI(source)) {
            sources.add(YamlSource.of(source));
        } else {
            sources.addAll(configs(source).stream()
                .map(YamlSource::of)
                .collect(toList()));
        }
    }

    private List<YamlSource> getConfigFromSources(List<String> newSources) throws ConfiguratorException {
        List<YamlSource> sources = new ArrayList<>();

        for (String p : newSources) {
            appendSources(sources, p);
        }
        return sources;
    }

    /**
     * Defaults to use a file in the current working directory with the name 'jenkins.yaml'
     *
     * Add the environment variable CASC_JENKINS_CONFIG to override the default. Accepts single file or a directory.
     * If a directory is detected, we scan for all .yml and .yaml files
     *
     * @throws Exception when the file provided cannot be found or parsed
     */
    @Restricted(NoExternalUse.class)
    @Initializer(after = InitMilestone.EXTENSIONS_AUGMENTED, before = InitMilestone.JOB_LOADED)
    public static void init() throws Exception {
        Long duration = Long.getLong(ConfigurationAsCode.class.getName() + ".initialDelay");
        if (duration != null) {
            try {
                Thread.sleep(duration);
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, "Interrupted whilst delaying CasC startup", e);
            }
        }
        detectVaultPluginMissing();
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
        List<YamlSource> configs = new ArrayList<>();

        List<String> standardConfig = getStandardConfig();
        for (String p : standardConfig) {
            appendSources(configs, p);
        }
        sources = Collections.unmodifiableList(standardConfig);
        return configs;
    }

    private List<String> getStandardConfig() {
        List<String> configParameters = getBundledCasCURIs();
        CasCGlobalConfig casc = GlobalConfiguration.all().get(CasCGlobalConfig.class);
        String cascPath = casc != null ? casc.getConfigurationPath() : null;

        String configParameter = System.getProperty(
                CASC_JENKINS_CONFIG_PROPERTY,
                System.getenv(CASC_JENKINS_CONFIG_ENV)
        );

        // We prefer to rely on environment variable over global config
        if (StringUtils.isNotBlank(cascPath) && StringUtils.isBlank(configParameter)) {
            configParameter = cascPath;
        }

        if (configParameter == null) {
            String fullPath = Jenkins.get().getRootDir() + File.separator + DEFAULT_JENKINS_YAML_PATH;
            if (Files.exists(Paths.get(fullPath))) {
                configParameter = fullPath;
            }
        }

        if (configParameter != null) {
            // Add external config parameter
            configParameters.add(configParameter);
        }

        if (configParameters.isEmpty()) {
            LOGGER.log(Level.FINE, "No configuration set nor default config file");
        }
        return configParameters;
    }

    @Restricted(NoExternalUse.class)
    public List<String> getBundledCasCURIs() {
        final String cascFile = "/WEB-INF/" + DEFAULT_JENKINS_YAML_PATH;
        final String cascDirectory = "/WEB-INF/" + DEFAULT_JENKINS_YAML_PATH + ".d/";
        List<String> res = new ArrayList<>();

        final ServletContext servletContext = Jenkins.get().servletContext;
        try {
            URL bundled = servletContext.getResource(cascFile);
            if (bundled != null) {
                res.add(bundled.toString());
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to load " + cascFile, e);
        }

        PathMatcher matcher = FileSystems.getDefault().getPathMatcher(YAML_FILES_PATTERN);
        Set<String> resources = servletContext.getResourcePaths(cascDirectory);
        if (resources!=null) {
            // sort to execute them in a deterministic order
            for (String cascItem : new TreeSet<>(resources)) {
                try {
                    URL bundled = servletContext.getResource(cascItem);
                    if (bundled != null && matcher.matches(new File(bundled.getPath()).toPath())) {
                        res.add(bundled.toString());
                    } //TODO: else do some handling?
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Failed to execute " + res, e);
                }
            }
        }

        return res;
    }

    @RequirePOST
    @Restricted(NoExternalUse.class)
    public void doCheck(StaplerRequest req, StaplerResponse res) throws Exception {

        if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        final Map<Source, String> issues = checkWith(YamlSource.of(req));
        res.setContentType("application/json");
        final JSONArray warnings = new JSONArray();
        issues.entrySet().stream().map(e -> new JSONObject().accumulate("line", e.getKey().line).accumulate("warning", e.getValue()))
                .forEach(warnings::add);
        warnings.write(res.getWriter());
    }

    @RequirePOST
    @Restricted(NoExternalUse.class)
    public void doApply(StaplerRequest req, StaplerResponse res) throws Exception {

        if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        configureWith(YamlSource.of(req));
    }

    /**
     * Export live jenkins instance configuration as Yaml
     * @throws Exception
     */
    @RequirePOST
    @Restricted(NoExternalUse.class)
    public void doExport(StaplerRequest req, StaplerResponse res) throws Exception {

        if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        res.setContentType("application/x-yaml; charset=utf-8");
        res.addHeader("Content-Disposition", "attachment; filename=jenkins.yaml");
        export(res.getOutputStream());
    }

    /**
     * Export JSONSchema to URL
     * @throws Exception
     */
    @Restricted(NoExternalUse.class)
    public void doSchema(StaplerRequest req, StaplerResponse res) throws Exception {

        if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        res.setContentType("application/json; charset=utf-8");
        res.getWriter().print(writeJSONSchema());
    }

    @RequirePOST
    @Restricted(NoExternalUse.class)
    public void doViewExport(StaplerRequest req, StaplerResponse res) throws Exception {
        if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        export(out);

        req.setAttribute("export", out.toString(StandardCharsets.UTF_8.name()));
        req.getView(this, "viewExport.jelly").forward(req, res);
    }

    @Restricted(NoExternalUse.class)
    public void doReference(StaplerRequest req, StaplerResponse res) throws Exception {
        if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        req.getView(this, "reference.jelly").forward(req, res);
    }

    @Restricted(NoExternalUse.class)
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
    @Restricted(NoExternalUse.class)
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
    @Restricted(NoExternalUse.class)
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

                final DumperOptions.ScalarStyle style;
                if (scalar.getFormat().equals(Format.MULTILINESTRING) && !scalar.isRaw()) {
                    style = LITERAL;
                } else if (scalar.isRaw()) {
                    style = PLAIN;
                } else {
                    style = DOUBLE_QUOTED;
                }

                return new ScalarNode(getTag(scalar.getFormat()), value, null, null, style);
        }
    }

    private Tag getTag(Scalar.Format format) {
        switch (format) {
            case NUMBER:
                return Tag.INT;
            case FLOATING:
                return Tag.FLOAT;
            case BOOLEAN:
                return Tag.BOOL;
            case STRING:
            case MULTILINESTRING:
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
            appendSources(configs, p);
        }
        sources = Collections.unmodifiableList(configParameters.stream().collect(toList()));
        configureWith(configs);
        lastTimeLoaded = System.currentTimeMillis();
    }

    public static boolean isSupportedURI(String configurationParameter) {
        if(configurationParameter == null) {
            return false;
        }
        final List<String> supportedProtocols = Arrays.asList("https","http","file");
        URI uri;
        try {
            uri = new URI(configurationParameter);
        } catch (URISyntaxException ex) {
            return false;
        }
        if(uri.getScheme() == null) {
            return false;
        }
        return supportedProtocols.contains(uri.getScheme());
    }

    @Restricted(NoExternalUse.class)
    public void configureWith(YamlSource source) throws ConfiguratorException {
        final List<YamlSource> sources = getStandardConfigSources();
        sources.add(source);
        configureWith(sources);
    }

    private void configureWith(List<YamlSource> sources) throws ConfiguratorException {
        lastTimeLoaded = System.currentTimeMillis();
        configureWith( YamlUtils.loadFrom(sources) );
        closeSources(sources);
    }

    @Restricted(NoExternalUse.class)
    public Map<Source, String> checkWith(YamlSource source) throws ConfiguratorException {
        final List<YamlSource> sources = getStandardConfigSources();
        sources.add(source);
        return checkWith(sources);
    }

    private Map<Source, String> checkWith(List<YamlSource> sources) throws ConfiguratorException {
        if (sources.isEmpty()) return Collections.emptyMap();
        return checkWith( YamlUtils.loadFrom(sources) );
    }

    private void closeSources(List<YamlSource> sources) {
        for (YamlSource source : sources) {
            try {
                source.close();
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to close YAML Source", e);
            }
        }
    }

    /**
     * Recursive search for all {@link #YAML_FILES_PATTERN} in provided base path
     *
     * @param path base path to start (can be file or directory)
     * @return list of all paths matching pattern. Only base file itself if it is a file matching pattern
     */
    @Restricted(NoExternalUse.class)
    public List<Path> configs(String path) throws ConfiguratorException {
        final Path root = Paths.get(path);

        if (!Files.exists(root)) {
            throw new ConfiguratorException("Invalid configuration: '"+path+"' isn't a valid path.");
        }

        if (Files.isRegularFile(root) && Files.isReadable(root)) {
            return Collections.singletonList(root);
        }

        final PathMatcher matcher = FileSystems.getDefault().getPathMatcher(YAML_FILES_PATTERN);
        try (Stream<Path> stream = Files.find(Paths.get(path), Integer.MAX_VALUE,
                (next, attrs) -> !attrs.isDirectory() && !isHidden(next) && matcher.matches(next))) {
            return stream.sorted().collect(toList());
        } catch (IOException e) {
            throw new IllegalStateException("failed config scan for " + path, e);
        }
    }

    private static boolean isHidden(Path path) {
        return IntStream.range(0, path.getNameCount())
            .mapToObj(path::getName)
            .anyMatch(subPath -> subPath.toString().startsWith("."));
    }

    @FunctionalInterface
    private interface ConfiguratorOperation {

        Object apply(RootElementConfigurator configurator, CNode node) throws ConfiguratorException;
    }

    /**
     * Configuration with help of {@link RootElementConfigurator}s.
     * Corresponding configurator is searched by entry key, passing entry value as object with all required properties.
     *
     * @param entries key-value pairs, where key should match to root configurator and value have all required properties
     * @throws ConfiguratorException configuration error
     */
    private static void invokeWith(Mapping entries, ConfiguratorOperation function) throws ConfiguratorException {

        // Run configurators by order, consuming entries until all have found a matching configurator.
        // Configurators order is important so that io.jenkins.plugins.casc.plugins.PluginManagerConfigurator run
        // before any other, and can install plugins required by other configuration to successfully parse yaml data
        for (RootElementConfigurator configurator : RootElementConfigurator.all()) {
            final Iterator<Map.Entry<String, CNode>> it = entries.entrySet().iterator();
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
            final Map.Entry<String, CNode> next = entries.entrySet().iterator().next();
            throw new ConfiguratorException(format("No configurator for root element <%s>", next.getKey()));
        }
    }

    private static void detectVaultPluginMissing() {
        PluginManager pluginManager = Jenkins.get().getPluginManager();
        Set<String> envKeys = System.getenv().keySet();
        if (envKeys.stream().anyMatch(s -> s.startsWith("CASC_VAULT_"))
            && pluginManager.getPlugin("hashicorp-vault-plugin") == null) {
            LOGGER.log(Level.SEVERE,
                "Vault secret resolver is not installed, consider installing hashicorp-vault-plugin v2.4.0 or higher\nor consider removing any 'CASC_VAULT_' variables");
        }
    }

    private void configureWith(Mapping entries) throws ConfiguratorException {
        // Initialize secret sources
        SecretSource.all().forEach(SecretSource::init);

        // Check input before actually applying changes,
        // so we don't let master in a weird state after some ConfiguratorException has been thrown
        final Mapping clone = entries.clone();
        checkWith(clone);

        final ObsoleteConfigurationMonitor monitor = ObsoleteConfigurationMonitor.get();
        monitor.reset();
        ConfigurationContext context = new ConfigurationContext(registry);
        context.addListener(monitor::record);
        try (ACLContext acl = ACL.as(ACL.SYSTEM)) {
            invokeWith(entries, (configurator, config) -> configurator.configure(config, context));
        }
    }

    public Map<Source, String> checkWith(Mapping entries) throws ConfiguratorException {
        Map<Source, String> issues = new HashMap<>();
        ConfigurationContext context = new ConfigurationContext(registry);
        context.addListener( (node,message) -> issues.put(node.getSource(), message) );
        invokeWith(entries, (configurator, config) -> configurator.check(config, context));
        return issues;
    }


    public static ConfigurationAsCode get() {
        return Jenkins.get().getExtensionList(ConfigurationAsCode.class).get(0);
    }

    /**
     * Used for documentation generation in index.jelly
     */
    public Collection<?> getRootConfigurators() {
        return new LinkedHashSet<>(RootElementConfigurator.all());
    }

    /**
     * Used for documentation generation in index.jelly
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
    @Restricted(NoExternalUse.class)
    @NonNull
    public String getHtmlHelp(Class type, String attribute) throws IOException {
        final URL resource = Klass.java(type).getResource("help-" + attribute + ".html");
        if (resource != null) {
            return IOUtils.toString(resource.openStream(), StandardCharsets.UTF_8);
        }
        return "";
    }

    /**
     * Retrieve which plugin do provide this extension point, used in documentation.jelly
     *
     * @return String representation of the extension source, usually artifactId.
     */
    @Restricted(NoExternalUse.class)
    @CheckForNull
    public String getExtensionSource(Configurator c) throws IOException {
        final Class e = c.getImplementedAPI();
        final String jar = Which.jarFile(e).getName();
        if (jar.startsWith("jenkins-core-")) { // core jar has version in name
            return "jenkins-core";
        }
        return jar.substring(0, jar.lastIndexOf('.'));
    }

    @Restricted(NoExternalUse.class)
    public static String printThrowable(@NonNull Throwable t) {
        String s = Functions.printThrowable(t)
            .split("at io.jenkins.plugins.casc.ConfigurationAsCode.export")[0]
            .replaceAll("\t", "  ");
        return s.substring(0, s.lastIndexOf(")") + 1);
    }

}
