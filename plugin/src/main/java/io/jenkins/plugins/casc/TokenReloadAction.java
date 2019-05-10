package io.jenkins.plugins.casc;

import com.google.common.base.Strings;
import hudson.Extension;
import hudson.model.UnprotectedRootAction;
import hudson.util.Scrambler;
import java.io.IOException;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;

/**
 * If enabled by setting environment variable "CASC_ALLOW_LOCAL_RELOAD" to "true" it allows reloading
 * configuration by issuing a POST request to "http://localhost:8080/reload-configuration-as-code/".
 */
@Extension
public class TokenReloadAction implements UnprotectedRootAction {
    public static final Logger LOGGER = Logger.getLogger(TokenReloadAction.class.getName());

    public static final String URL_NAME = "/reload-configuration-as-code";
    public static final String RELOAD_TOKEN = "jcasc.reloadToken";


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
        String token = getReloadToken();

        if (Strings.isNullOrEmpty(token)) {
            LOGGER.fine("reload via token is not enabled");
        } else {
            String requestToken = getRequestToken(request);

            if (token.equals(requestToken)) {
                LOGGER.info("configuration reload triggered via token");
                ConfigurationAsCode.get().configure();
            } else {
                response.sendError(HttpStatus.SC_UNAUTHORIZED);
                LOGGER.warning("unauthorized to reload configuration");

            }
        }
    }

    private String getRequestToken(HttpServletRequest request) {
        final String authorization = request.getHeader("Authorization");
        if (!StringUtils.startsWithIgnoreCase(authorization, "Basic ")) {
            return null;
        }

        String uidpassword = Scrambler.descramble(authorization.substring(6));
        int idx = uidpassword.indexOf(':');
        if (idx >= 0) {
            return uidpassword.substring(idx + 1);

        }
        return null;
    }

    private static String getReloadToken() {
        return System.getProperty(RELOAD_TOKEN);
    }

    public static boolean tokenReloadEnabled() {
        return !Strings.isNullOrEmpty(getReloadToken());
    }

}
