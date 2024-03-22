package io.jenkins.plugins.casc;

import hudson.util.BootFailure;
import java.io.IOException;
import javax.servlet.ServletException;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class ConfigurationAsCodeBootFailure extends BootFailure {

    public ConfigurationAsCodeBootFailure(ConfiguratorException cause) {
        super(cause);
    }

    public void doDynamic(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        rsp.setStatus(503);
        ConfigurationAsCode.handleExceptionOnReloading(req, rsp, (ConfiguratorException) getCause());
    }
}
