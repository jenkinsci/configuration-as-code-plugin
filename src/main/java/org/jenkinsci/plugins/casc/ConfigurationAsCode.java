package org.jenkinsci.plugins.casc;

import hudson.Extension;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.ManagementLink;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.CheckForNull;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.*;
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
    @Initializer(after = InitMilestone.EXTENSIONS_AUGMENTED)
    public static void init() throws Exception {
        get().configure();
    }

    /**
     * Main entry point to start configuration process
     */
    public void configure() {
        String configParameter = System.getProperty(
                CASC_JENKINS_CONFIG_PROPERTY,
                System.getenv(CASC_JENKINS_CONFIG_ENV)
        );

        if(isSupportedURI(configParameter)) {
            _configureWithURI(configParameter);
        } else {
            //Must be a plain path
            _configureWithPaths(configParameter);
        }
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

    private void _configureWithURI(String configParameter) {
        try {
            URL url = URI.create(configParameter).toURL();
            try(BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
                entries(reader).forEach(ConfigurationAsCode::configureWith);
                sources = Collections.singletonList(configParameter);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to read from URL: "+configParameter, e);
            }
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Failed to read from url. Url is malformed: "+configParameter, e);
        }
    }

    private void _configureWithPaths(String configParameter) {
        List<Path> configs = configs(configParameter);
        sources = configs.stream().map(Path::toAbsolutePath).map(Path::toString).collect(toList());
        configs.stream()
                .flatMap(ConfigurationAsCode::entries)
                .forEach(ConfigurationAsCode::configureWith);
    }

    /**
     * Recursive search for all {@link #YAML_FILES_PATTERN} in provided base path
     *
     * @param path base path to start (can be file or directory)
     * @return list of all paths matching pattern. Only base file itself if it is a file matching pattern
     */
    public List<Path> configs(String path) {
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher(YAML_FILES_PATTERN);

        try (Stream<Path> configs = Files.find(
                Paths.get(StringUtils.defaultIfBlank(path, DEFAULT_JENKINS_YAML_PATH)),
                Integer.MAX_VALUE,
                (next, attrs) -> attrs.isRegularFile() && matcher.matches(next)
        )) {
            return configs.collect(toList());
        } catch (NoSuchFileException e) {
            return Collections.emptyList();
        } catch (IOException e) {
            throw new IllegalStateException("failed config scan for " + path, e);
        }
    }

    /**
     * Creates the stream of configurable entries in config in form [root key -> some object]
     *
     * @param config path to read from
     * @return stream of entries from yaml
     */
    private static Stream<? extends Map.Entry<String, Object>> entries(Path config) {
        try (Reader reader = Files.newBufferedReader(config)) {
            return entries(reader);
        } catch (IOException e) {
            throw new IllegalStateException(e);
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
     */
    public static void configureWith(Map.Entry<String, Object> entry) {
        RootElementConfigurator configurator = Objects.requireNonNull(
                Configurator.lookupRootElement(entry.getKey()),
                format("no configurator for root element <%s>", entry.getKey())
        );
        try {
            configurator.configure(entry.getValue());
        } catch (Exception e) {
            throw new IllegalStateException(
                    format("error configuring <%s> with <%s> configurator", entry.getKey(), configurator.getName()),
                    e
            );
        }
    }


    public static ConfigurationAsCode get() {
        return Jenkins.getInstance().getExtensionList(ConfigurationAsCode.class).get(0);
    }

    // for documentation generation in index.jelly
    public List<?> getConfigurators() {
        List<Object> elements = new ArrayList<>();
        for (RootElementConfigurator c : RootElementConfigurator.all()) {
            elements.add(c);
            listElements(elements, c.describe());
        }
        return elements;
    }

    // for documentation generation in index.jelly
    public List<?> getRootConfigurators() {
        return RootElementConfigurator.all();
    }

    private void listElements(List<Object> elements, Set<Attribute> attributes) {
        for (Attribute attribute : attributes) {

            final Class type = attribute.type;
            Configurator configurator = Configurator.lookup(type);
            if (configurator == null ) {
                continue;
            }
            for (Object o : configurator.getConfigurators()) {
                if (!elements.contains(o)) {
                    elements.add(o);
                }
            }
            listElements(elements, configurator.describe());
        }
    }
}
