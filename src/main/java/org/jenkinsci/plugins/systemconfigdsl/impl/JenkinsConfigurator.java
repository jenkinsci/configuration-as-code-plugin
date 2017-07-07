package org.jenkinsci.plugins.systemconfigdsl.impl;

import com.google.auto.service.AutoService;
import hudson.util.DescribableList;
import org.jenkinsci.plugins.systemconfigdsl.api.Configurator;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import jenkins.model.*;
import java.util.logging.Logger;
import java.net.InetAddress;
import hudson.slaves.EnvironmentVariablesNodeProperty;

/**
 * Created by ewelinawilkosz on 07/07/2017.
 */

@AutoService(Configurator.class)
public class JenkinsConfigurator extends Configurator {

    private static final Logger LOGGER = Logger.getLogger(JenkinsConfigurator.class.getName());

    @Override
    public String getConfigFileSectionName() {
        return "jenkins";
    }

    @Override
    public void configure(Object config, boolean dryRun) {
        LOGGER.info("Configuring jenkins: " + config.toString());
        Map map = (Map)config;
        JenkinsLocationConfiguration jlc = JenkinsLocationConfiguration.get();

        if (dryRun == true) {
            LOGGER.info("DryRun: Only print what you will change");
        } else {
            LOGGER.info("Applying configuration...");

            try {
                LOGGER.info("--> set number of executors on master to " + map.get("numExecutorsOnMaster"));
                Jenkins.getInstance().setNumExecutors(Integer.parseInt(map.get("numExecutorsOnMaster").toString()));

                LOGGER.info("--> set quite period to  " + map.get("scmQuietPeriod"));
                Jenkins.getInstance().setQuietPeriod(Integer.parseInt(map.get("scmQuietPeriod").toString()));

                LOGGER.info("--> set checkout retry to  " + map.get("scmCheckoutRetryCount"));
                Jenkins.getInstance().setScmCheckoutRetryCount(Integer.parseInt(map.get("scmCheckoutRetryCount").toString()));


                // Set Admin Email as a string "Name <email>"
                if (map.get("jenkinsAdminEmail").toString().length() > 0) {
                    LOGGER.info("--> set admin e-mail address to  " + map.get("jenkinsAdminEmail"));
                    jlc.setAdminAddress(map.get("jenkinsAdminEmail").toString());
                    jlc.save();
                }

                // Change it to the DNS name if you have it
                if (map.get("jenkinsRootUrl").toString().length() > 0) {
                    LOGGER.info("--> set jenkins root url to ${properties.global.jenkinsRootUrl}");
                    jlc.setUrl(map.get("jenkinsRootUrl").toString());
                } else {
                    String ip = InetAddress.getLocalHost().getHostAddress();
                    LOGGER.info("--> set jenkins root url to " + ip);
                    jlc.setUrl("http://" + ip + ":8080");
                }
                jlc.save();

                LOGGER.info("--> set global env variables  " + map.get("variables"));
                Map variables = (Map) map.get("variables");
                Iterator it = variables.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry variable = (Map.Entry)it.next();
                    addGlobalEnvVariable(variable.getKey(), variable.getValue());
                }


            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean isConfigurationValid(Object config) {
        return true;
    }

    private void addGlobalEnvVariable(Object key, Object value) {
        Jenkins instance = Jenkins.getInstance();
        LOGGER.info("--> envVarsNodePropertyList " + instance.getGlobalNodeProperties().getAll(EnvironmentVariablesNodeProperty.class));
        if ( instance.getGlobalNodeProperties().getAll(EnvironmentVariablesNodeProperty.class).isEmpty()) {
            EnvironmentVariablesNodeProperty.Entry entry = new EnvironmentVariablesNodeProperty.Entry(key.toString(), value.toString());
            instance.getGlobalNodeProperties().add(new EnvironmentVariablesNodeProperty(entry));
        } else {
            instance.getGlobalNodeProperties().getAll(EnvironmentVariablesNodeProperty.class).get(0).getEnvVars().put(key.toString(), value.toString());
        }
        LOGGER.info("--> added global environment variable " + key + " = " + value);
        try {
            instance.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
