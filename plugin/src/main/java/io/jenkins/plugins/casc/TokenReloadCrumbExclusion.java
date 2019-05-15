package io.jenkins.plugins.casc;

import hudson.Extension;
import hudson.security.csrf.CrumbExclusion;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Extension
public class TokenReloadCrumbExclusion extends CrumbExclusion {

    @Override
    public boolean process(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {

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
