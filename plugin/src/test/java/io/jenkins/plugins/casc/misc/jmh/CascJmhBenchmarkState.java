package io.jenkins.plugins.casc.misc.jmh;

import io.jenkins.plugins.casc.ConfigurationAsCode;
import io.jenkins.plugins.casc.ConfiguratorException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import jenkins.benchmark.jmh.JmhBenchmarkState;

/**
 * Use Configuration as Code to setup the Jenkins instance for JMH benchmark.
 */
public abstract class CascJmhBenchmarkState extends JmhBenchmarkState {
    private static final Logger LOGGER = Logger.getLogger(CascJmhBenchmarkState.class.getName());

    /**
     * Location of the YAML file to be used for configuration-as-code
     *
     * @return String containing the location of YAML file in the classpath
     */
    @Nonnull
    protected abstract String getResourcePath();

    /**
     * Setups the Jenkins instance using configuration as code
     * available through the {@link CascJmhBenchmarkState#getResourcePath()}.
     *
     * @throws ConfiguratorException when unable to configure
     */
    @Override
    public void setup() throws Exception {
        String config = Objects.requireNonNull(getClass().getClassLoader().getResource(getResourcePath())).toExternalForm();
        try {
            ConfigurationAsCode.get().configure(config);
        } catch (ConfiguratorException e) {
            LOGGER.log(Level.SEVERE, "Unable to configure using configuration as code. Aborting.");
            terminateJenkins();
            throw e; // causes JMH to abort benchmark
        }
    }
}
