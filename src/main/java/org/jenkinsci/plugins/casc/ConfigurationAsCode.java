package org.jenkinsci.plugins.casc;

import hudson.Plugin;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import org.apache.commons.lang.StringUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class ConfigurationAsCode extends Plugin {

    /**
     * Defaults to use a file in the current working directory with the name 'jenkins.yaml'
     *
     * Add the environment variable CASC_JENKINS_CONFIG to override the default. Accepts URI's to statically served files
     * from URL or local file paths. Use 'http://..' 'https://..' file://..' or '/home/jenkins/jenkins.yaml'
     *
     * @throws Exception when the URI or file provided cannot be found or parsed
     */
    @Initializer(after = InitMilestone.EXTENSIONS_AUGMENTED)
    public static void configure() throws Exception {
        final String configParameter = System.getenv("CASC_JENKINS_CONFIG");
        final InputStream is = getConfigurationInput(configParameter);
        if (is != null) {
            configure(is);
        }
    }


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

    public static InputStream getConfigurationInput(String configUri) throws IOException {
        InputStream is;
        if(StringUtils.isBlank(configUri)) {
            is = new FileInputStream(new File("./jenkins.yaml"));
        } else {
            URI uri = URI.create(configUri);
            if(uri.getScheme() != null && (uri.getScheme().equals("http") || uri.getScheme().equals("https") || uri.getScheme().equals("file"))) {
                is = uri.toURL().openStream();
            } else {
                //Must be a file path then. Try to open it.
                is = new FileInputStream(new File(configUri));
            }
        }
        return is;
    }
}
