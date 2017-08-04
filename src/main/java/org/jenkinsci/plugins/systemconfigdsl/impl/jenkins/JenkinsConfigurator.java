package org.jenkinsci.plugins.systemconfigdsl.impl.jenkins;

import com.google.auto.service.AutoService;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import jenkins.model.Jenkins;
import jenkins.model.JenkinsLocationConfiguration;
import org.jenkinsci.plugins.systemconfigdsl.api.Configurator;
import org.jenkinsci.plugins.systemconfigdsl.impl.jenkins.generated.EnvVariable;
import org.jenkinsci.plugins.systemconfigdsl.impl.jenkins.generated.JenkinsConfig;

import java.io.IOException;
import java.net.InetAddress;
import java.util.logging.Logger;

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
    public void configure(final String config, boolean dryRun) {
        LOGGER.info("Configuring jenkins: " + config);
        JenkinsLocationConfiguration jlc = JenkinsLocationConfiguration.get();
        JenkinsConfig jenkinsConfig = (JenkinsConfig) super.parseConfiguration(config, JenkinsConfig.class);

        if (dryRun == true) {
            LOGGER.info("DryRun: Only print what you will change");
        } else {
            LOGGER.info("Applying configuration...");

            try {
                if (jenkinsConfig.getNumExecutorsOnMaster() != 0) {
                    LOGGER.info("--> set number of executors on master to " + jenkinsConfig.getNumExecutorsOnMaster().toString());
                    Jenkins.getInstance().setNumExecutors(jenkinsConfig.getNumExecutorsOnMaster());
                }
                if (jenkinsConfig.getScmQuietPeriod() != 0) {
                    LOGGER.info("--> set quite period to  " + jenkinsConfig.getScmQuietPeriod().toString());
                    Jenkins.getInstance().setQuietPeriod(jenkinsConfig.getScmQuietPeriod());
                }
                if (jenkinsConfig.getScmCheckoutRetryCount() != 0) {
                    LOGGER.info("--> set checkout retry to  " + jenkinsConfig.getScmCheckoutRetryCount().toString());
                    Jenkins.getInstance().setScmCheckoutRetryCount(jenkinsConfig.getScmCheckoutRetryCount());
                }

                // Set Admin Email as a string "Name <email>"
                if (jenkinsConfig.getJenkinsAdminEmail() != "") {
                    LOGGER.info("--> set admin e-mail address to  " + jenkinsConfig.getJenkinsAdminEmail());
                    jlc.setAdminAddress(jenkinsConfig.getJenkinsAdminEmail());
                    jlc.save();
                }

                // Change it to the DNS name if you have it
                if (jenkinsConfig.getJenkinsRootUrl() != "") {
                    LOGGER.info("--> set jenkins root url to " + jenkinsConfig.getJenkinsRootUrl());
                    jlc.setUrl(jenkinsConfig.getJenkinsRootUrl());
                } else {
                    String ip = InetAddress.getLocalHost().getHostAddress();
                    LOGGER.info("--> set jenkins root url to http://" + ip + ":8080");
                    jlc.setUrl("http://" + ip + ":8080");
                }
                jlc.save();

                LOGGER.info("--> set global env variables");
                for (EnvVariable variable: jenkinsConfig.getVariables()) {
                    addGlobalEnvVariable(variable.getName(), variable.getValue());
                }
                if (jenkinsConfig.getSystemMessage() != "") {
                    LOGGER.info("--> set system message");
                    Jenkins.getInstance().setSystemMessage(jenkinsConfig.getSystemMessage());
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean isConfigurationValid(final String config) {
        boolean status = true;
        if (! super.isSchemaValid(config, this.getConfigFileSectionName())) {
            LOGGER.warning("Provided configuration doesn't match schema");
            status = false;
        }
        return status;
    }

    private void addGlobalEnvVariable(final String key, final String value) {
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
