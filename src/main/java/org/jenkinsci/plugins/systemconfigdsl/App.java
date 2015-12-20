package org.jenkinsci.plugins.systemconfigdsl;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import hudson.init.Initializer;
import jenkins.model.Jenkins;
import org.codehaus.groovy.control.CompilerConfiguration;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
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
                return name.endsWith(".conf");
            }
        });
        if (files==null)    return;

        Arrays.sort(files);

        CompilerConfiguration cc = new CompilerConfiguration();
        cc.setScriptBaseClass(ConfigScript.class.getName());
        GroovyShell sh = new GroovyShell(jenkins.pluginManager.uberClassLoader,new Binding(),cc);

        for (File f : files) {
            try {
                ConfigScript s = (ConfigScript) sh.parse(f);
                s.setDelegate(new Surrogate(jenkins));
                s.run();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to execute " + f, e);
            }
        }
    }

    private static final Logger LOGGER = Logger.getLogger(App.class.getName());
}
