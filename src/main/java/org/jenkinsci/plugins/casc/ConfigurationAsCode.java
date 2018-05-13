package org.jenkinsci.plugins.casc;

import hudson.Extension;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.ManagementLink;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.casc.yaml.YamlReader;
import org.jenkinsci.plugins.casc.yaml.YamlSource;
import org.jenkinsci.plugins.casc.yaml.YamlUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.composer.Composer;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Node;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
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
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    public static final String DEFAULT_JENKINS_YAML_PATH = "./jenkins.yaml";
    public static final String YAML_FILES_PATTERN = "glob:**.{yml,yaml,YAML,YML}";

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
        return "An opinionated way to configure jenkins based on human-readable declarative configuration files";
    }

    private long lastTimeLoaded;

    private List<String> sources = Collections.emptyList();

    public Date getLastTimeLoaded() {
        return new Date(lastTimeLoaded);
    }

    public List<String> getSources() {
        return sources;
    }

    @RequirePOST
    public void doReload(StaplerRequest request, StaplerResponse response) throws Exception {
        if (!Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER)) {
            return;
        }

        configure();
        response.sendRedirect("");
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
     * Main entry point to start configuration process
     * @throws ConfiguratorException Configuration error
     */
    public void configure() throws ConfiguratorException {
        String configParameter = System.getProperty(
                CASC_JENKINS_CONFIG_PROPERTY,
                System.getenv(CASC_JENKINS_CONFIG_ENV)
        );
        if (configParameter == null  && Files.exists(Paths.get(DEFAULT_JENKINS_YAML_PATH))) {
            configParameter = DEFAULT_JENKINS_YAML_PATH;
        } else {
            // No configuration set nor default config file
            return;
        }

        configure(configParameter);
    }


    public void configure(String ... configParameters) throws ConfiguratorException {

        List<YamlSource> configs = new ArrayList<>();

        for (String p : configParameters) {
            if (isSupportedURI(p)) {
                configs.add(new YamlSource<>(p, READ_FROM_URL));
            } else {
                configs.addAll(configs(p).stream()
                        .map(s -> new YamlSource<>(s, READ_FROM_PATH))
                        .collect(toList()));
            }
        }
        configureWith(configs);
        lastTimeLoaded = System.currentTimeMillis();
    }

    private static final YamlReader<String> READ_FROM_URL = config -> {
        final URL url = URI.create(config).toURL();
        return new InputStreamReader(url.openStream(), "UTF-8");
    };

    private static final YamlReader<Path> READ_FROM_PATH = Files::newBufferedReader;


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

    private void configureWith(List<YamlSource> configs) throws ConfiguratorException {
        final Node merged = YamlUtils.merge(configs);
        final Map<String, Object> map = loadAs(merged);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            configureWith(entry);
        }
    }

    private Map<String, Object> loadAs(Node node) {
        final Constructor constructor = new Constructor();
        constructor.setComposer(new Composer(null, null) {

            @Override
            public Node getSingleNode() {
                return node;
            }
        });
        return (Map<String, Object>) constructor.getSingleData(Map.class);
    }



    /**
     * Recursive search for all {@link #YAML_FILES_PATTERN} in provided base path
     *
     * @param path base path to start (can be file or directory)
     * @return list of all paths matching pattern. Only base file itself if it is a file matching pattern
     */
    public List<Path> configs(String path) throws ConfiguratorException {
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher(YAML_FILES_PATTERN);

        try (Stream<Path> stream = Files.find(Paths.get(path), Integer.MAX_VALUE,
                (next, attrs) -> attrs.isRegularFile() && matcher.matches(next))) {
            return stream.collect(toList());
        } catch (NoSuchFileException e) {
            throw new ConfiguratorException("File does not exist: " + path, e);
        } catch (IOException e) {
            throw new IllegalStateException("failed config scan for " + path, e);
        }
    }

    private static Stream<? extends Map.Entry<String, Object>> entries(Reader config) {
        return ((Map<String, Object>) new Yaml().loadAs(config, Map.class)).entrySet().stream();
    }

    /**
     * Configuration with help of {@link RootElementConfigurator}s.
     * Corresponding configurator is searched by entry key, passing entry value as object with all required properties.
     *
     * @param entry key-value pair, where key should match to root configurator and value have all required properties
     * @throws ConfiguratorException configuration error
     */
    public static void configureWith(Map.Entry<String, Object> entry) throws ConfiguratorException {

        RootElementConfigurator configurator = Configurator.lookupRootElement(entry.getKey());
        if (configurator == null) {
            throw new ConfiguratorException(format("no configurator for root element <%s>", entry.getKey()));
        }
        try {
            configurator.configure(entry.getValue());
        } catch (ConfiguratorException e) {
            throw new ConfiguratorException(
                    configurator,
                    format("error configuring <%s> with <%s> configurator", entry.getKey(), configurator.getName()),
                    e
            );
        }
    }


    public static ConfigurationAsCode get() {
        return Jenkins.getInstance().getExtensionList(ConfigurationAsCode.class).get(0);
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
        Set<Object> elements = new LinkedHashSet<>(roots);
        for (RootElementConfigurator root : roots) {
            listElements(elements, root.describe());
        }
        return elements;
    }

    /**
     * Recursive configurators tree walk (DFS).
     * Collects all configurators starting from root ones in {@link #getConfigurators()}
     *
     * @param elements   linked set (to save order) of visited elements
     * @param attributes siblings to find associated configurators and dive to next tree levels
     */
    private void listElements(Set<Object> elements, Set<Attribute> attributes) {
        attributes.stream()
                .map(Attribute::getType)
                .map(Configurator::lookup)
                .filter(Objects::nonNull)
                .forEach(configurator -> {
                    elements.addAll(configurator.getConfigurators());
                    listElements(elements, configurator.describe());
                });
    }
}
