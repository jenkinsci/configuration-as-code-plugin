package io.jenkins.plugins.casc;

import hudson.Extension;
import hudson.security.csrf.CrumbExclusion;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Extension
public class TokenReloadCrumbExclusion extends CrumbExclusion {

    @Override
    public boolean process(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (TokenReloadAction.tokenReloadEnabled()) {
            String pathInfo = request.getPathInfo();
            if (pathInfo != null && pathInfo.equals("/" + TokenReloadAction.URL_NAME + "/")) {
                chain.doFilter(request, response);
                return true;
            }
        }
        return false;
    }
}
