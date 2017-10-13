package org.jenkinsci.plugins.systemconfigdsl;

import hudson.ExtensionList;
import hudson.PluginWrapper;
import hudson.init.Initializer;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.logging.Logger;

import static hudson.init.InitMilestone.EXTENSIONS_AUGMENTED;
import static hudson.init.InitMilestone.JOB_LOADED;

@SuppressWarnings("unused") // loaded by Jenkins
public class OnStartupConfigurationApplicator {
    private static final Logger LOGGER = Logger.getLogger(OnStartupConfigurationApplicator.class.getName());

    @Initializer(after = EXTENSIONS_AUGMENTED, before = JOB_LOADED)
    public static void init(Jenkins jenkins) throws IOException {

        /*
            The goal:
                Be able to structure YAML file to match Jenkins UI
            Practical implementation:
                Can we use a plugin's DataboundSetters, public properties or jelly to configure it?

                Brainstorm #1
                1. call DataboundSetters on plugins directly

                Brainstorm #2
                1. Parse jelly
                2. Build config using jelly/YAML file.
                3. Configure plugin with  built config
         */

        LOGGER.info("--------------------------------------------------------");
        LOGGER.info("--------------------------------------------------------");
        LOGGER.info("--------------------------------------------------------");

        LOGGER.info("OnStartupConfigurationApplicator.init");

        ExtensionList<GlobalConfiguration> globalConfigurations = jenkins.getExtensionList(GlobalConfiguration.class);
        for (GlobalConfiguration globalConfig : globalConfigurations) {
            LOGGER.info("globalConfig: " + globalConfig.getDisplayName());

            Constructor[] constructors = globalConfig.getClass().getDeclaredConstructors();
            for (Constructor constructor : constructors) {
                LOGGER.info("CONSTRUCTOR: " + constructor.getName());
                Annotation[] annotations = constructor .getDeclaredAnnotations();
                for (Annotation annotation : annotations) {
                    LOGGER.info(" - ANNOTATION - " + annotation.annotationType().getName());
                }
            }

            Method[] methods = globalConfig.getClass().getDeclaredMethods();
            for (Method method : methods) {
                LOGGER.info("METHOD: " + method.getName());
                Annotation[] annotations = method.getDeclaredAnnotations();
                for (Annotation annotation : annotations) {
                    LOGGER.info(" - ANNOTATION - " + annotation.annotationType().getName());
                }
            }
        }

        LOGGER.info("--------------------------------------------------------");
        LOGGER.info("--------------------------------------------------------");
        LOGGER.info("--------------------------------------------------------");

        for (PluginWrapper plugin : jenkins.getPluginManager().getPlugins()) {
            LOGGER.info("plugin: " + plugin.getDisplayName());
        }
    }
}
