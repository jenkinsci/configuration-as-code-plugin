package io.jenkins.plugins.casc;

import hudson.util.BootFailure;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;

public class ConfigurationAsCodeBootFailure extends BootFailure {

    public ConfigurationAsCodeBootFailure(ConfiguratorException cause) {
        super(cause);
    }

    public void doDynamic(StaplerRequest2 req, StaplerResponse2 rsp) throws IOException, ServletException {
        rsp.setStatus(503);
        ConfigurationAsCode.handleExceptionOnReloading(req, rsp, (ConfiguratorException) getCause());
    }
}
