package io.jenkins.plugins.casc.impl.secrets;

import hudson.Extension;
import io.jenkins.plugins.casc.SecretSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * This {@link SecretSource} implementation allows to use a .properties file for providing secrets.
 * The default file path is {@code /run/secrets/secrets.properties}, which can be changed via
 * {@code SECRETS_FILE} environment variable.
 *
 * @author <a href="mailto:d.estermann.de@gmail.com">Daniel Estermann</a>
 * @since 1.33
 */
@Extension
@Restricted(NoExternalUse.class)
public class PropertiesSecretSource extends SecretSource {

    private static final Logger LOGGER = Logger.getLogger(PropertiesSecretSource.class.getName());

    /**
     * Default path for .properties file
     */
    public static final String SECRETS_DEFAULT_PATH = "/run/secrets/secrets.properties";

    private Properties secrets;

    @Override
    public Optional<String> reveal(String secret) {
        // lazy initialization
        if (secrets == null) {
            secrets = new Properties();
            final String secretsEnv = System.getenv("SECRETS_FILE");
            final String secretsPath = secretsEnv == null ? SECRETS_DEFAULT_PATH : secretsEnv;
            final File secretsFile = new File(secretsPath);
            if (secretsFile.exists() && secretsFile.isFile()) {
                try (InputStream input = new FileInputStream(secretsFile)) {
                    secrets.load(input);
                } catch (IOException ioe) {
                    LOGGER.log(Level.WARNING,
                        "Source properties file " + secretsPath + " could not be loaded", ioe);
                }
            }
        }

        if (secrets.getProperty(secret) == null) {
            return Optional.empty();
        } else {
            return Optional.of(secrets.getProperty(secret));
        }
    }
}
