package org.jenkinsci.plugins.systemconfigdsl.impl.security;

import com.google.auto.service.AutoService;
import hudson.security.ACL;
import hudson.security.AuthorizationStrategy;
import hudson.security.FullControlOnceLoggedInAuthorizationStrategy;
import hudson.security.HudsonPrivateSecurityRealm;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.systemconfigdsl.api.Configurator;
import org.jenkinsci.plugins.systemconfigdsl.impl.credentials.CredentialsConfigurator;
import org.jenkinsci.plugins.systemconfigdsl.impl.credentials.generated.UsernamePasswordCrendetialsConfig;
import org.jenkinsci.plugins.systemconfigdsl.impl.security.generated.SecurityOwnDBConfig;
import org.jenkinsci.plugins.systemconfigdsl.impl.security.generated.SecurityOwnDBUsers;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.logging.Logger;

/**
 *
 */
@AutoService(Configurator.class)
public class SecurityConfigurator extends Configurator{

    private static final Logger LOGGER = Logger.getLogger(CredentialsConfigurator.class.getName());
    private final String pluginName = "securityOwnDB";
    @Override
    public String getConfigFileSectionName() {
        return this.pluginName;
    }

    @Override
    public void configure(String config, boolean dryRun) {
        LOGGER.info("Configuring OwnDB security: " + config.toString());
        if (dryRun == true) {
            LOGGER.info("DryRun: Only print what you will change");
            // TODO: add printout to UI
        } else {
            HudsonPrivateSecurityRealm realm = new HudsonPrivateSecurityRealm(false, false, null);
            final SecurityOwnDBConfig configObject = (SecurityOwnDBConfig) super.parseConfiguration(config, SecurityOwnDBConfig.class);
            for(SecurityOwnDBUsers user : configObject.getUsers()){
                try{
                    realm.createAccount(user.getUserId(),new String((Files.readAllBytes(Paths.get(user.getPath())))).trim());
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                Jenkins.getInstance().setSecurityRealm(realm);
                // TODO: A ?hack? to prevent unsecure authorization. By setting security realm, all security settings
                // turning off, by default it sets anyone can do anything. Is it a bug?? Need to investigate!
                // Also it sets Allowing Jenkins CLI to work and master security subsystem is off.
                Jenkins.getInstance().setAuthorizationStrategy(new FullControlOnceLoggedInAuthorizationStrategy());
                Jenkins.getInstance().save();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            LOGGER.info("Applying configuration...");
        }

    }
    @Override
    public boolean isConfigurationValid(String config) {
        return false;
    }
}
