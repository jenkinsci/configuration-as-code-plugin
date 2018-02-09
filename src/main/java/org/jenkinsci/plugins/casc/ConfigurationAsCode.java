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
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

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

    @CheckForNull
    @Override
    public String getIconFileName() {
        return "images/48x48/setting.png";
    }

    @CheckForNull
    @Override
    public String getDisplayName() {
        return "configuration-as-code";
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return "configuration-as-code";
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

    public void configure() throws Exception {
        final String path = System.getProperty(
                CASC_JENKINS_CONFIG_PROPERTY,
                System.getenv(CASC_JENKINS_CONFIG_ENV)
        );

        List<Path> configs = configs(path);
        sources = configs.stream().map(Path::toAbsolutePath).map(Path::toString).collect(toList());
        configs.stream()
                .flatMap(ConfigurationAsCode::entries)
                .forEach(ConfigurationAsCode::configureWith);

        lastTimeLoaded = System.currentTimeMillis();
    }

    public List<Path> configs(String path) throws IOException {
        try (Stream<Path> configs = Files.find(
                Paths.get(StringUtils.defaultIfBlank(path, DEFAULT_JENKINS_YAML_PATH)),
                Integer.MAX_VALUE,
                (next, attrs) -> next.toString().endsWith(".yml") || next.toString().endsWith(".yaml")
        )) {
            return configs.collect(toList());
        } catch (NoSuchFileException e) {
            return Collections.emptyList();
        }
    }

    private static Stream<? extends Map.Entry<String, Object>> entries(Path config) {
        try (Reader reader = Files.newBufferedReader(config)) {
            return ((Map<String, Object>) new Yaml().loadAs(reader, Map.class)).entrySet().stream();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void configureWith(Map.Entry<String, Object> entry) {
        RootElementConfigurator configurator = Objects.requireNonNull(
                Configurator.lookupRootElement(entry.getKey()),
                "no configurator for root element '" + entry.getKey() + "'"
        );
        try {
            configurator.configure(entry.getValue());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }


    public static ConfigurationAsCode get() {
        return Jenkins.getInstance().getExtensionList(ConfigurationAsCode.class).get(0);
    }

    // for documentation generation in index.jelly
    public List<?> getConfigurators() {
        Stream<RootElementConfigurator> roots = RootElementConfigurator.all().stream();

        Stream<Configurator<?>> children = roots
                .flatMap(root -> root.describe().stream())
                .flatMap(attribute -> attribute.configurators());

        Stream<Configurator> configurators = children.flatMap(configurator -> configurator.flattened());

        return Stream.concat(roots, configurators).distinct().collect(toList());
    }

    // for documentation generation in index.jelly
    public List<?> getRootConfigurators() {
        return RootElementConfigurator.all();
    }
}
