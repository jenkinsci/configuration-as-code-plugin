package io.jenkins.plugins.casc.misc.jmh;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jenkins.plugins.casc.ConfigurationAsCode;
import io.jenkins.plugins.casc.ConfiguratorException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.benchmark.jmh.JmhBenchmark;
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
    @NonNull
    protected abstract String getResourcePath();

    /**
     * The class containing this benchmark state. The config is loaded using this class's {@link Class#getResource(String)}.
     *
     * @return the class containing this benchmark state
     */
    @NonNull
    protected abstract Class<?> getEnclosingClass();

    /**
     * Setups the Jenkins instance using configuration as code
     * available through the {@link CascJmhBenchmarkState#getResourcePath()}.
     *
     * @throws ConfiguratorException when unable to configure
     */
    @Override
    public void setup() throws Exception {
        Class<?> enclosingClass = getEnclosingClass();

        if (!enclosingClass.isAnnotationPresent(JmhBenchmark.class)) {
            throw new IllegalStateException("The enclosing class must be annotated with @JmhBenchmark");
        }

        String config = Objects.requireNonNull(getEnclosingClass().getResource(getResourcePath()),
                "Unable to find YAML config file").toExternalForm();
        try {
            ConfigurationAsCode.get().configure(config);
        } catch (ConfiguratorException e) {
            LOGGER.log(Level.SEVERE, "Unable to configure using configuration as code. Aborting.");
            terminateJenkins();
            throw e; // causes JMH to abort benchmark
        }
    }
}
