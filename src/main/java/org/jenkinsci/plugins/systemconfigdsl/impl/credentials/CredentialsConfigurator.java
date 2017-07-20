package org.jenkinsci.plugins.systemconfigdsl.impl.credentials;

import com.cloudbees.plugins.credentials.*;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.google.auto.service.AutoService;
import hudson.ExtensionList;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.systemconfigdsl.Validator;
import org.jenkinsci.plugins.systemconfigdsl.api.Configurator;
import org.jenkinsci.plugins.systemconfigdsl.error.ValidationException;
import org.jenkinsci.plugins.systemconfigdsl.impl.credentials.generated.UsernamePasswordCrendetials;
import org.jenkinsci.plugins.systemconfigdsl.impl.credentials.generated.UsernamePasswordCrendetialsConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@AutoService(Configurator.class)
public class CredentialsConfigurator extends Configurator {

    private static final Logger LOGGER = Logger.getLogger(CredentialsConfigurator.class.getName());
    private final String pluginName = "credentials";

    @Override
    public String getConfigFileSectionName() {
        return this.pluginName;
    }

    @Override
    public void configure(final String config, boolean dryRun) {
        LOGGER.info("Configuring credentials: " + config.toString());
        /*if (! isConfigurationValid(config)) {
            LOGGER.warning("Provided configuration isn't valid. can't configure what you asked me to");
            return;
        }*/
        if (dryRun == true) {
            LOGGER.info("DryRun: Only print what you will change");
            // TODO: add printout to UI
        } else {
            final UsernamePasswordCrendetialsConfig configObject = (UsernamePasswordCrendetialsConfig) super.parseConfiguration(config, UsernamePasswordCrendetialsConfig.class);
            for (UsernamePasswordCrendetials cred: configObject.getCredentials()) {
                SystemCredentialsProvider.ProviderImpl system = ExtensionList.lookup(CredentialsProvider.class).get(
                        SystemCredentialsProvider.ProviderImpl.class);
                CredentialsStore systemStore = system.getStore(Jenkins.getInstance());

                List<Credentials> credentialsList = new ArrayList<Credentials>(systemStore.getCredentials(Domain.global()));
                // TODO: check that credentials already exists and then only modify them
                UsernamePasswordCredentialsImpl upc = null;
                try {
                    // TODO: handle credentials scope
                    upc = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL,
                            cred.getCredentialsId(),
                            cred.getDescription(),
                            cred.getCredentialsId(),
                            new String((Files.readAllBytes(Paths.get(cred.getPath())))).trim());
                    systemStore.addCredentials(Domain.global(), upc);
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
}
