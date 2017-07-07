package org.jenkinsci.plugins.systemconfigdsl;

import hudson.PluginWrapper;
import jenkins.model.Jenkins;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Utils {

    private static final Logger LOGGER = Logger.getLogger(Utils.class.getName());

    // Dynamically load classes to avoid dependency to class imports
    public static Class getClassbyName(final String className) {
        Class result = null;
        try {
             result = Class.forName(className);
        } catch (ClassNotFoundException ex) {
            LOGGER.warning("ERROR: Can not find class " + className);

        }
        return result;
    }

    public static boolean isPluginInstalled(final String pluginName) {
        final List<PluginWrapper> installedPlugins = Jenkins.getInstance().getPluginManager().getPlugins();
        List<PluginWrapper> result = installedPlugins.stream()
                .filter(plugin -> plugin.getShortName().equals(pluginName))
                .collect(Collectors.toList());
        return true ? result.size() != 0 : false;
    }

    public static String getAsString(final String propertyName, final Object config, final String def) {
        Map configAsMap = (Map) config;
        return configAsMap.get(propertyName) != null ? configAsMap.get(propertyName).toString()  : def;
    }

    public static String getAsStringOrDie(final String propertyName, final Object config) throws NoSuchFieldException {
        Map configAsMap = (Map) config;
        if (! configAsMap.containsKey(propertyName) || configAsMap.get(propertyName) == null) {
            throw new NoSuchFieldException("Provided configuration " + config.toString() + " not contains key " + propertyName.toString());
        }
        return configAsMap.get(propertyName).toString();
    }

    public static Integer getAsInteger(final String propertyName, final Object config, final Integer def) {
        Map configAsMap = (Map) config;
        return configAsMap.get(propertyName) != null ? Integer.parseInt(configAsMap.get(propertyName).toString())  : def;
    }
}
