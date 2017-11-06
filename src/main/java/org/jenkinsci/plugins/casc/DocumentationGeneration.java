package org.jenkinsci.plugins.casc;

import jenkins.model.Jenkins;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * TODO rely on some JenkinsRule or comparable to generate this statically from jenkins-core version + set of plugins
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class DocumentationGeneration {


    // @Initializer(after = InitMilestone.EXTENSIONS_AUGMENTED)
    public static void init() {
        new DocumentationGeneration().generate();
    }

    private final List<Class> documented = new ArrayList<>();

    public void generate() {

        documented.add(int.class);
        documented.add(String.class);
        documented.add(boolean.class);
        documented.add(Integer.class);
        // ...

        for (RootElementConfigurator c : ConfigurationAsCode.getRootConfigurators()) {
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
