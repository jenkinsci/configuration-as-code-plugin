package org.jenkinsci.plugins.systemconfigdsl.impl;

import com.cloudbees.plugins.credentials.*;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.google.auto.service.AutoService;
import hudson.ExtensionList;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.systemconfigdsl.Utils;
import org.jenkinsci.plugins.systemconfigdsl.api.Configurator;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@AutoService(Configurator.class)
public class CredentialsConfigurator extends Configurator {

    private static final Logger LOGGER = Logger.getLogger(CredentialsConfigurator.class.getName());

    @Override
    public String getConfigFileSectionName() {
        return "credentials";
    }

    @Override
    public void configure(Object config, boolean dryRun) {
        LOGGER.info("Configuring credentials: " + config.toString());
        if (! Utils.isPluginInstalled("credentials")) {
            LOGGER.warning("credentials plugins is not installed. can't configure what you asked me to");
            return;
        }
        if (! isConfigurationValid(config)) {
            LOGGER.warning("Provided configuration isn't valid. can't configure what you asked me to");
            return;
        }
        if (dryRun == true) {
            LOGGER.info("DryRun: Only print what you will change");
            // TODO: add printout to UI
        } else {
            for (Object cred: (List) config) {
                SystemCredentialsProvider.ProviderImpl system = ExtensionList.lookup(CredentialsProvider.class).get(
                        SystemCredentialsProvider.ProviderImpl.class);
                CredentialsStore systemStore = system.getStore(Jenkins.getInstance());

                List<Credentials> credentialsList = new ArrayList<Credentials>(systemStore.getCredentials(Domain.global()));
                // TODO: check that credentials already exists and then only modify them
                UsernamePasswordCredentialsImpl creds = null;
                try {
                    creds = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL,
                            Utils.getAsStringOrDie("credentialsId", cred),
                            Utils.getAsString("description", cred, ""),
                            Utils.getAsStringOrDie("userId", cred),
                            new String((Files.readAllBytes(Paths.get(Utils.getAsStringOrDie("path", cred))))).trim());
                    systemStore.addCredentials(Domain.global(), creds);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public boolean isConfigurationValid(Object config) {
        boolean status = true;
        UsernamePasswordCredentialsStructure structure = new UsernamePasswordCredentialsStructure();
        // TODO: get a warnign about non-used fileds from configuration
        for (Object cred: (List) config) {
            for(Field field : structure.getClass().getFields()){
                if(! ((Map) cred).containsKey(field.getName())){
                    LOGGER.info("Provided configuration isn't valid. Filed " + field.getName() + ". Provided config: " + cred.toString());
                    status = false;
                    continue;
                }
            }
        }
        return status;
    }
}
