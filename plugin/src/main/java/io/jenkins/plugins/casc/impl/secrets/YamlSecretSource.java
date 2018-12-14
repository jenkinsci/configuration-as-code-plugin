package io.jenkins.plugins.casc.impl.secrets;

import hudson.Extension;
import io.jenkins.plugins.casc.SecretSource;
import io.jenkins.plugins.casc.snakeyaml.Yaml;
import jenkins.model.Jenkins;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
public class YamlSecretSource extends SecretSource {

    private final static Logger LOGGER = Logger.getLogger(YamlSecretSource.class.getName());

    public static final String CASC_SECRETS_CONFIG_PROPERTY = "casc.secrets.config";
    public static final String CASC_SECRETS_CONFIG_ENV = "CASC_SECRETS_CONFIG";
    public static final String DEFAULT_JENKINS_YAML_PATH = "secrets.yml";

    private String secretsFile = "";
    private Map<String, Object> secretsMap;

    public YamlSecretSource() {
        this.secretsFile = getStandardConfig();
        loadYaml();
    }

    private void loadYaml() {
        File file = new File(this.secretsFile);
        if(file.exists()) {
            Yaml yaml = new Yaml();
            try(InputStream inputStream = new BufferedInputStream(new FileInputStream(this.secretsFile))) {
                secretsMap = yaml.load(inputStream);
            } catch (FileNotFoundException e) {
                LOGGER.info("Secrets yaml file not found");
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error accessing secrets file");
            }
        }
    }

    private String getStandardConfig() {
        String configParameters = "";

        String configParameter = System.getProperty(
                CASC_SECRETS_CONFIG_PROPERTY,
                System.getenv(CASC_SECRETS_CONFIG_ENV)
        );

        if (configParameter == null) {
            String fullPath = Jenkins.getInstance().getRootDir() + File.separator + DEFAULT_JENKINS_YAML_PATH;
            if (Files.exists(Paths.get(fullPath))) {
                configParameter = fullPath;
            }
        }

        if (configParameter != null) {
            configParameters = configParameter;
        }

        if (configParameters.equals("")) {
            LOGGER.log(Level.FINE, "No secrets configuration set");
        }
        return configParameters;
    }

    @Override
    public Optional<String> reveal(String secret) {

        if(secretsMap!=null) {

            if (secretsMap.containsKey("secrets")) {
                Map<String, Object> secrets = (Map<String, Object>) secretsMap.get("secrets");
                if (secrets.containsKey(secret)) {
                    return Optional.of((String) secrets.get(secret));
                }
            }
        }

        return Optional.empty();
    }
}
