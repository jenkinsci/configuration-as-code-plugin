package org.jenkinsci.plugins.casc;

import hudson.Plugin;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * {@linkplain #configure() Main entry point of the logic}.
 *
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class ConfigurationAsCode extends Plugin {

    /**
     * Defaults to use a file in the current working directory with the name 'jenkins.yaml'
     *
     * Add the environment variable CASC_JENKINS_CONFIG to override the default. Accepts single file or a directory.
     * If a directory is detected, we scan for all .yml and .yaml files
     *
     * @throws Exception when the file provided cannot be found or parsed
     */
    @Initializer(after = InitMilestone.EXTENSIONS_AUGMENTED)
    public static void configure() throws Exception {
        final String configParameter = System.getenv("CASC_JENKINS_CONFIG");
        final List<InputStream> is = getConfigurationInput(configParameter);
        for(InputStream isToConfigure : is) {
            if (isToConfigure != null) {
                configure(isToConfigure);
            }
        }
    }

    /**
     * Reads YAML from the given {@link InputStream} and applies that to Jenkins.
     */
    public static void configure(InputStream in) throws Exception {
        Map<String, Object> config = new Yaml().loadAs(in, Map.class);
        for (Map.Entry<String, Object> e : config.entrySet()) {
            final RootElementConfigurator configurator = Configurator.lookupRootElement(e.getKey());
            if (configurator == null) {
                throw new IllegalArgumentException("no configurator for root element '"+e.getKey()+"'");
            }
            configurator.configure(e.getValue());
        }
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

    public static List<InputStream> getConfigurationInput(String configPath) throws IOException {
        List<InputStream> is = new ArrayList<>();
        //Default
        if(StringUtils.isBlank(configPath)) {
            File defaultConfig = new File("./jenkins.yaml");
            if(defaultConfig.exists()) {
                is = Arrays.asList(new FileInputStream(new File("./jenkins.yaml")));
            } else {
                is = Collections.EMPTY_LIST;
            }
        } else {
            File cfg = new File(configPath);
            if(cfg.isDirectory()) {
                for(File cfgFile : FileUtils.listFiles(cfg, new String[]{"yml","yaml"},true)) {
                    is.add(new FileInputStream(cfgFile));
                }
            } else {
                is = Arrays.asList(new FileInputStream(cfg));
            }
        }
        return is;
    }
}
