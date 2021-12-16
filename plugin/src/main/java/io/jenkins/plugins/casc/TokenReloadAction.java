package io.jenkins.plugins.casc;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.Extension;
import hudson.model.UnprotectedRootAction;
import hudson.security.ACL;
import hudson.security.ACLContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
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

        if (token == null || token.isEmpty()) {
            response.sendError(404);
            LOGGER.warning("Configuration reload via token is not enabled");
        } else {
            String requestToken = getRequestToken(request);

            if (requestToken != null && MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8), requestToken.getBytes(
                StandardCharsets.UTF_8))) {
                LOGGER.info("Configuration reload triggered via token");

                try (ACLContext ignored = ACL.as(ACL.SYSTEM)) {
                    ConfigurationAsCode.get().configure();
                }
            } else {
                response.sendError(401);
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
        String token = getReloadTokenProperty();
        return token != null && !token.isEmpty();
    }
}
