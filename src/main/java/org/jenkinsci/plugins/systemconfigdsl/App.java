package org.jenkinsci.plugins.systemconfigdsl;

import com.esotericsoftware.yamlbeans.YamlReader;
import hudson.init.Initializer;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.systemconfigdsl.api.Configurator;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

import static hudson.init.InitMilestone.*;

/**
 * @author Kohsuke Kawaguchi
 */
public class App {
    private final Jenkins jenkins = Jenkins.getInstance();

    @Initializer(after=EXTENSIONS_AUGMENTED,before=JOB_LOADED)
    public static void init(Jenkins j) throws IOException {
        new App().run(new File(Jenkins.getInstance().getRootDir(),"conf"));
    }

    public void run(File dir) throws IOException {
        File[] files = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".yml");
            }
        });
        if (files==null)    return;

        final Map<String,Configurator> supportedPlugins = new HashMap<>();
        final ServiceLoader<Configurator> loader = ServiceLoader.load(Configurator.class, Jenkins.getInstance().getPluginManager().uberClassLoader);
        Iterator<Configurator> recipeIterator = loader.iterator();
        while (recipeIterator.hasNext()) {
            Configurator configurator = recipeIterator.next();
            LOGGER.warning("Found configurator implementation: " + configurator.getConfigFileSectionName());
            supportedPlugins.put(configurator.getConfigFileSectionName(), configurator);
        }

        for (File file: files) {
            YamlReader reader = new YamlReader(new FileReader(file));
            Map config = (Map) reader.read();
            for (Object key: config.keySet()) {
                if (supportedPlugins.containsKey(key)) {
                    System.out.println("configuration section " + key.toString() + " supported");
                    supportedPlugins.get(key).configure(config.get(key));
                }
            }
        }
    }

    private static final Logger LOGGER = Logger.getLogger(App.class.getName());
}
