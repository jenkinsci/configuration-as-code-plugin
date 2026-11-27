package io.jenkins.plugins.casc.encryption;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import io.jenkins.plugins.casc.BaseConfigurator;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.RootElementConfigurator;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import java.util.Map;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * Configures external decryption settings.
 * Example:
 * decryption:
 *   jwe:
 *     keyFile: /run/secrets/casc/private-key.pem
 */
@Extension
public class DecryptionRootConfigurator extends BaseConfigurator<DecryptionRootConfigurator.DecryptionConfiguration>
        implements RootElementConfigurator<DecryptionRootConfigurator.DecryptionConfiguration> {

    private static final String ROOT_NAME = "decryption";

    @Override
    @NonNull
    public String getName() {
        return ROOT_NAME;
    }

    @Override
    public Class<DecryptionConfiguration> getTarget() {
        return DecryptionConfiguration.class;
    }

    @Override
    public DecryptionConfiguration getTargetComponent(ConfigurationContext context) {
        return new DecryptionConfiguration();
    }

    @Override
    protected DecryptionConfiguration instance(Mapping mapping, ConfigurationContext context) {
        return new DecryptionConfiguration();
    }

    @Override
    protected void configure(
            Mapping config, DecryptionConfiguration instance, boolean dryrun, ConfigurationContext context)
            throws ConfiguratorException {
        super.configure(config, instance, false, context);
    }

    @Override
    @NonNull
    public DecryptionConfiguration configure(CNode config, ConfigurationContext context) throws ConfiguratorException {

        DecryptionConfiguration configuration = super.configure(config, context);
        applyConfiguration(configuration, config, context);

        return configuration;
    }

    @Override
    @NonNull
    public DecryptionConfiguration check(CNode config, ConfigurationContext context) throws ConfiguratorException {

        DecryptionConfiguration configuration = super.check(config, context);
        applyConfiguration(configuration, config, context);

        return configuration;
    }

    /**
     * Initializes the decryption settings before the normal root configurators
     * are executed. This is necessary because secret interpolation happens
     * during the configuration validation phase.
     */
    public static void initialize(Mapping entries, ConfigurationContext context) throws ConfiguratorException {

        CNode decryptionNode = null;
        boolean found = false;

        for (Map.Entry<String, CNode> entry : entries.entrySet()) {
            if (ROOT_NAME.equalsIgnoreCase(entry.getKey())) {
                if (found) {
                    throw ConfiguratorException.from(entries, "Duplicate '" + ROOT_NAME + "' configuration section.");
                }

                decryptionNode = entry.getValue();
                found = true;
            }
        }

        if (found) {
            CNode nodeToCheck = decryptionNode != null ? decryptionNode.clone() : null;
            new DecryptionRootConfigurator().check(nodeToCheck, context);
        }
    }

    private void applyConfiguration(DecryptionConfiguration configuration, CNode config, ConfigurationContext context)
            throws ConfiguratorException {

        JweConfiguration jwe = configuration.getJwe();

        if (jwe == null) {
            throw ConfiguratorException.from(config, "Missing required configuration: 'decryption.jwe.keyFile'.");
        }

        context.setJweKeyFileFromConfiguration(jwe.getKeyFile());
    }

    @Override
    @CheckForNull
    public CNode describe(DecryptionConfiguration instance, ConfigurationContext context) {
        return null;
    }

    public static class DecryptionConfiguration {

        private JweConfiguration jwe;

        public JweConfiguration getJwe() {
            return jwe;
        }

        @DataBoundSetter
        @SuppressWarnings("unused")
        public void setJwe(JweConfiguration jwe) {
            this.jwe = jwe;
        }
    }

    @SuppressWarnings("ClassCanBeRecord")
    public static class JweConfiguration {

        private final String keyFile;

        @DataBoundConstructor
        public JweConfiguration(String keyFile) {
            if (keyFile == null || keyFile.isBlank()) {
                throw new IllegalArgumentException("'decryption.jwe.keyFile' must be a non-empty file path.");
            }

            this.keyFile = keyFile.trim();
        }

        public String getKeyFile() {
            return keyFile;
        }
    }
}
