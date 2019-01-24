package io.jenkins.plugins.casc;

import hudson.Extension;
import hudson.security.csrf.CrumbExclusion;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Extension
public class LocalReloadCrumbExclusion extends CrumbExclusion {

    @Override
    public boolean process(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {

        if (LocalReloadAction.localReloadEnabled()) {
            String pathInfo = request.getPathInfo();
            if (pathInfo != null && pathInfo.equals(LocalReloadAction.URL_NAME + "/")) {
                chain.doFilter(request, response);
                return true;
            }
        }
        return false;
    }
}
