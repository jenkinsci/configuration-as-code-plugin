package org.jenkinsci.plugins.systemconfigdsl;

import groovy.lang.GroovyShell;
import hudson.init.Initializer;
import jenkins.model.Jenkins;

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

        GroovyShell sh = new GroovyShell();
        sh.setProperty("jenkins",new Surrogate(Jenkins.getInstance()));
        for (File f : files) {
            try {
                sh.evaluate(f);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to execute " + f, e);
            }
        }
    }

    private static final Logger LOGGER = Logger.getLogger(App.class.getName());
}
