package io.jenkins.plugins.casc;

import com.google.common.base.Strings;
import hudson.Extension;
import hudson.model.UnprotectedRootAction;
import hudson.security.ACL;
import hudson.security.ACLContext;
import java.io.IOException;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.httpclient.HttpStatus;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;

@Extension
public class TokenReloadAction implements UnprotectedRootAction {
    public static final Logger LOGGER = Logger.getLogger(TokenReloadAction.class.getName());

    public static final String URL_NAME = "reload-configuration-as-code";
    public static final String RELOAD_TOKEN_PROPERTY = "casc.reload.token";
    public static final String RELOAD_TOKEN_QUERY_PARAMETER = "casc-reload-token";

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
        String token = getReloadTokenProperty();

        if (Strings.isNullOrEmpty(token)) {
            response.sendError(HttpStatus.SC_NOT_FOUND);
            LOGGER.warning("Configuration reload via token is not enabled");
        } else {
            String requestToken = getRequestToken(request);

            if (token.equals(requestToken)) {
                LOGGER.info("Configuration reload triggered via token");

                try (ACLContext ignored = ACL.as(ACL.SYSTEM)) {
                    ConfigurationAsCode.get().configure();
                }
            } else {
                response.sendError(HttpStatus.SC_UNAUTHORIZED);
                LOGGER.warning("Invalid token received, not reloading configuration");

            }
        }
    }

    private String getRequestToken(HttpServletRequest request) {
        return request.getParameter(RELOAD_TOKEN_QUERY_PARAMETER);
    }

    private static String getReloadTokenProperty() {
        return System.getProperty(RELOAD_TOKEN_PROPERTY);
    }

    public static boolean tokenReloadEnabled() {
        return !Strings.isNullOrEmpty(getReloadTokenProperty());
    }
}
