package io.jenkins.plugins.casc.impl.secrets;

import hudson.Extension;
import io.jenkins.plugins.casc.SecretSource;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This {@link SecretSource} implementation allows to use a .properties file for providing secrets.
 * The default file path is {@code /run/secrets/secrets.properties}, which can be changed via
 * {@code SECRETS} environment variable.
 *
 * @author <a href="mailto:d.estermann.de@gmail.com">Daniel Estermann</a>
 */
@Extension
public class PropertiesSecretSource extends SecretSource {

    private static final Logger LOGGER = Logger.getLogger(PropertiesSecretSource.class.getName());

    /**
     * Default path for .properties file
     */
    public static final String SECRETS_DEFAULT_PATH = "/run/secrets/secrets.properties";

    private final Properties secrets = new Properties();

    public PropertiesSecretSource() {
        final String secretsEnv = System.getenv("SECRETS");
        final String secretsPath = secretsEnv == null ? SECRETS_DEFAULT_PATH : secretsEnv;
        try (InputStream input = new FileInputStream(secretsPath)) {
            secrets.load(input);
        }
        catch (FileNotFoundException fnfe) {
            LOGGER.log(Level.WARNING, "Source properties file has not been found.", fnfe);
        }
        catch (IOException ioe) {
            LOGGER.log(Level.WARNING, "Source properties file could not be loaded.", ioe);
        }
    }

    @Override
    public Optional<String> reveal(String secret) throws IOException {
        if (secrets.getProperty(secret) == null) {
            return Optional.empty();
        }
        else {
            return Optional.of(secrets.getProperty(secret));
        }
    }
}
