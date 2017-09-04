package org.jenkinsci.plugins.casc;

import hudson.Plugin;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import jenkins.model.Jenkins;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
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
            final Configurator configurator = Configurator.lookupRootElement(e.getKey());
            if (configurator == null) {
                throw new IllegalArgumentException("no configurator for root element "+e.getKey());
            }
            configurator.configure(e.getValue());
        }
    }

    public List<?> getConfigurators() {
        List<Object> elements = new ArrayList<>();
        for (RootElementConfigurator c : Jenkins.getInstance().getExtensionList(RootElementConfigurator.class)) {
            elements.add(c);
            listElements(elements, c.describe());
        }
        return elements;
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




    private final List<Class> documented = new ArrayList<>();
    {

        documented.add(int.class);
        documented.add(String.class);
        documented.add(boolean.class);
        documented.add(Integer.class);
        // ...

        for (RootElementConfigurator c : Jenkins.getInstance().getExtensionList(RootElementConfigurator.class)) {
            final String name = c.getName();
            document(name, c.describe());
        }
    }

    private void document(String name, Set<Attribute> attributes) {

        Set<Class> next = new HashSet<>();
        System.out.println();
        System.out.println("## " + name);
        for (Attribute attribute : attributes) {

            // FIXME filter attribute to target a component without anything configurable

            final Class type = attribute.getType();
            System.out.print("**"+attribute.getName() + "**  (");
            if (attribute.isMultiple())
                System.out.print("list of ");
            System.out.println(type+")");
            if (!attribute.possibleValues().isEmpty()) {
                System.out.println("possible values :");
                for (Object o : attribute.possibleValues()) {
                    System.out.println(" - " + o);
                }
            }

            if (! documented.contains(type)) {
                next.add(type);
            }
        }

        for (Class type : next) {
            Configurator configurator = Configurator.lookup(type);
            if (configurator == null) continue;
            document(type.getName(), configurator.describe());
        }


    }
}
