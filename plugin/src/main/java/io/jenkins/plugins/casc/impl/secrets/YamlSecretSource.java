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

    public YamlSecretSource() {
        this.secretsFile = getStandardConfig();
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
    public Optional<String> reveal(String secret) throws IOException {

        File file = new File(this.secretsFile);
        if(!file.exists()) {
            return Optional.empty();
        }

        Yaml yaml = new Yaml();
        InputStream inputStream = new BufferedInputStream(new FileInputStream(this.secretsFile));
        Map<String, Object> obj = yaml.load(inputStream);

        if(obj.containsKey("secrets")) {
            Map<String, Object> secrets = (Map<String, Object>)obj.get("secrets");
            if(secrets.containsKey(secret)) {
                return Optional.of((String)secrets.get(secret));
            }
        }

        return Optional.empty();
    }
}
