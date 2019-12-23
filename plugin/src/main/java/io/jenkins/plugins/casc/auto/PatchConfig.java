package io.jenkins.plugins.casc.auto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.flipkart.zjsonpatch.JsonDiff;
import com.flipkart.zjsonpatch.JsonPatch;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import jenkins.model.Jenkins;
import org.apache.commons.io.IOUtils;

/**
 * Apply the patch between two versions of the initial config files
 */
public class PatchConfig {
    private static final Logger LOGGER = Logger.getLogger(CasCBackup.class.getName());

    final static  String DEFAULT_JENKINS_YAML_PATH = "jenkins.yaml";
    final static String cascFile = "/WEB-INF/" + DEFAULT_JENKINS_YAML_PATH;
    final static String cascDirectory = "/WEB-INF/" + DEFAULT_JENKINS_YAML_PATH + ".d/";
    final static  String cascUserConfigFile = "user.yaml";

    @Initializer(after= InitMilestone.STARTED, fatal=false)
    public static void patchConfig() {
        LOGGER.fine("start to calculate the patch of casc");

        URL newSystemConfig = findConfig("/" + DEFAULT_JENKINS_YAML_PATH);
        URL systemConfig = findConfig(cascFile);
        URL userConfig = findConfig(cascDirectory + cascUserConfigFile);
        URL userConfigDir = findConfig(cascDirectory);

        if (newSystemConfig == null || userConfigDir == null) {
            LOGGER.warning("no need to upgrade the configuration of Jenkins");
            return;
        }

        JsonNode patch = null;
        if (systemConfig != null && userConfig != null) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                JsonNode source = objectMapper.readTree(yamlToJson(systemConfig.openStream()));
                JsonNode target = objectMapper.readTree(yamlToJson(userConfig.openStream()));

                patch = JsonDiff.asJson(source, target);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "error happen when calculate the patch", e);
                return;
            }

            try {
                // give systemConfig a real path
                PatchConfig.copyAndDelSrc(newSystemConfig, systemConfig);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "error happen when copy the new system config", e);
                return;
            }
        }

        if (patch != null) {
            File userYamlFile = new File(userConfigDir.getFile(), "user.yaml");
            File userJSONFile = new File(userConfigDir.getFile(), "user.json");

            try (InputStream newSystemInput = systemConfig.openStream();
                 OutputStream userFileOutput = new FileOutputStream(userYamlFile);
                 OutputStream patchFileOutput = new FileOutputStream(userJSONFile)){
                ObjectMapper jsonReader = new ObjectMapper();
                JsonNode target = JsonPatch.apply(patch, jsonReader.readTree(yamlToJson(newSystemInput)));

                String userYaml = jsonToYaml(new ByteArrayInputStream(target.toString().getBytes()));

                userFileOutput.write(userYaml.getBytes());
                patchFileOutput.write(patch.toString().getBytes());
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "error happen when copy the new system config", e);
            }
        } else {
            LOGGER.warning("there's no patch of casc");
        }
    }

    private static URL findConfig(String path) {
        final ServletContext servletContext = Jenkins.getInstance().servletContext;
        try {
            return servletContext.getResource(path);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, String.format("error happen when finding path %s", path), e);
        }
        return null;
    }

    private static void copyAndDelSrc(URL src, URL target) throws IOException {
        try {
            PatchConfig.copy(src, target);
        } finally {
            boolean result = new File(src.getFile()).delete();
            LOGGER.fine("src file delete " + result);
        }
    }

    private static void copy(URL src, URL target) throws IOException {
        IOUtils.copy(src.openStream(), new FileOutputStream(target.getFile()));
    }

    private static String jsonToYaml(InputStream input) throws IOException {
        ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
        ObjectMapper jsonReader = new ObjectMapper();

        Object obj = jsonReader.readValue(input, Object.class);

        return yamlReader.writeValueAsString(obj);
    }

    private  static String yamlToJson(InputStream input) throws IOException {
        ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
        ObjectMapper jsonReader = new ObjectMapper();

        Object obj = yamlReader.readValue(input, Object.class);

        return jsonReader.writeValueAsString(obj);
    }
}
