package org.jenkinsci.plugins.casc.configurationsource;

import hudson.Extension;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Implements a plain file source as configuration. Either a folder or file
 */
@Extension
public class FileSource extends ConfigurationSource {

    public static final String CASC_JENKINS_CONFIG_ENV = "CASC_JENKINS_CONFIG";
    public static final String DEFAULT_JENKINS_YAML_PATH = "./jenkins.yaml";
    public static final String CASC_JENKINS_CONFIG_PROPERTY = "casc.jenkins.config";

    @Override
    public Map<String, InputStream> listSources() throws IOException {
        Map<String,InputStream> sources = new HashMap<>();
        String configParameter = System.getProperty(
                CASC_JENKINS_CONFIG_PROPERTY,
                System.getenv(CASC_JENKINS_CONFIG_ENV)
        );
        if(StringUtils.isBlank(configParameter)) {
            File defaultConfig = new File(DEFAULT_JENKINS_YAML_PATH);
            if(defaultConfig.exists()) {
                return Collections.singletonMap(defaultConfig.getName(), new FileInputStream(defaultConfig));
            } else {
                return Collections.emptyMap();
            }
        }
        File cfg = new File(configParameter);
        if(cfg.isDirectory()) {
            for(File cfgFile : FileUtils.listFiles(cfg, new String[]{"yml","yaml"},true)) {
                sources.put(cfgFile.getName(), new FileInputStream(cfgFile));
            }
        } else {
            sources.put(cfg.getName(), new FileInputStream(cfg));
        }
        return sources;
    }
}
