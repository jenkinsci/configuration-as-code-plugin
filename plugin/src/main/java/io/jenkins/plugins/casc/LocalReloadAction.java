package io.jenkins.plugins.casc;

import hudson.Extension;
import hudson.model.UnprotectedRootAction;
import java.io.IOException;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import org.apache.commons.httpclient.HttpStatus;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;

/**
 * If enabled by setting environment variable "CASC_ALLOW_LOCAL_RELOAD" to "true" it allows reloading
 * configuration by issuing a POST request to "http://localhost:8080/reload-configuration-as-code/".
 */
@Extension
public class LocalReloadAction implements UnprotectedRootAction {
    public static final Logger LOGGER = Logger.getLogger(LocalReloadAction.class.getName());

    public static final String URL_NAME = "/reload-configuration-as-code";
    public static final String CASC_ALLOW_LOCAL_RELOAD_ENV = "CASC_ALLOW_LOCAL_RELOAD";


    @CheckForNull
    @Override
    public String getIconFileName() {
        return null;
    }

    @CheckForNull
    @Override
    public String getDisplayName() {
        return "Reload Configuration as Code";
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return URL_NAME;
    }

    @RequirePOST
    public void doIndex(StaplerRequest request, StaplerResponse response) throws IOException {
        if (localReloadEnabled()) {
            String remoteAddr = request.getRemoteAddr();
            String localAddr = request.getLocalAddr();
            if (remoteAddr.equals(localAddr)) {
                LOGGER.info("local reload triggered from '" + remoteAddr + "'");
                ConfigurationAsCode.get().configure();
            } else {
                LOGGER.warning("unauthorized access from '" + remoteAddr + "'");
                response.sendError(HttpStatus.SC_UNAUTHORIZED);
            }
        } else {
            LOGGER.fine("local reload is not enabled");
        }
    }

    public static boolean localReloadEnabled() {
        return System.getenv().getOrDefault(CASC_ALLOW_LOCAL_RELOAD_ENV, "false").equals("true");
    }

}
