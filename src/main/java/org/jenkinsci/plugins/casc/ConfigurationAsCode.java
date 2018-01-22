package org.jenkinsci.plugins.casc;

import hudson.Plugin;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class ConfigurationAsCode extends Plugin {


    @Initializer(after = InitMilestone.EXTENSIONS_AUGMENTED)
    public static void configure() throws Exception {
        final File f = new File("./jenkins.yaml");
        if (f.exists()) {
            configure(new FileInputStream(f));
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
}
