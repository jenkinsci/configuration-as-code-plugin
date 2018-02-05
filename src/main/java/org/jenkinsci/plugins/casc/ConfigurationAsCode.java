package org.jenkinsci.plugins.casc;

import hudson.Extension;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.ManagementLink;
import jenkins.model.Jenkins;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.CheckForNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@linkplain #configure() Main entry point of the logic}.
 *
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension
public class ConfigurationAsCode extends ManagementLink {



    @CheckForNull
    @Override
    public String getIconFileName() {
        return "images/48x48/setting.png";
    }

    @CheckForNull
    @Override
    public String getDisplayName() {
        return "configuration-as-code";
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return "configuration-as-code";
    }

    private Date lastTimeLoaded;

    private List<String> sources = Collections.EMPTY_LIST;

    public Date getLastTimeLoaded() {
        return lastTimeLoaded;
    }

    public List<String> getSources() {
        return sources;
    }

    @RequirePOST
    public void doReload(StaplerRequest request, StaplerResponse response) throws Exception {
        if (!Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER)) {
            return;
        }

        configure();
        response.sendRedirect(getUrlName());
    }

    /**
     * Defaults to use a file in the current working directory with the name 'jenkins.yaml'
     *
     * Add the environment variable CASC_JENKINS_CONFIG to override the default. Accepts single file or a directory.
     * If a directory is detected, we scan for all .yml and .yaml files
     *
     * @throws Exception when the file provided cannot be found or parsed
     */
    @Initializer(after = InitMilestone.EXTENSIONS_AUGMENTED)
    public static void init() throws Exception {
        get().configure();
    }


    public void configure() throws Exception {
        List<String> files = new ArrayList<>();

        final String configParameter = System.getenv("CASC_JENKINS_CONFIG");
        final Map<String, InputStream> is = getConfigurationInputs(configParameter);
        for (Map.Entry<String, InputStream> e : is.entrySet()) {
            files.add(e.getKey());
            configure(e.getValue());
        }
        sources = files;
        lastTimeLoaded = new Date();
    }

    public static ConfigurationAsCode get() {
        return Jenkins.getInstance().getExtensionList(ConfigurationAsCode.class).get(0);
    }

    /**
     * Reads YAML from the given {@link InputStream} and applies that to Jenkins.
     */
    public static void configure(InputStream in) throws Exception {
        Map<String, Object> config = new Yaml().loadAs(in, Map.class);
        for (Map.Entry<String, Object> e : config.entrySet()) {
            final RootElementConfigurator configurator = Configurator.lookupRootElement(e.getKey());
            if (configurator == null) {
                throw new IllegalArgumentException("no configurator for root element '"+e.getKey()+"'");
            }
            configurator.configure(e.getValue());
        }
    }

    // for documentation generation in index.jelly
    public List<?> getConfigurators() {
        List<Object> elements = new ArrayList<>();
        for (RootElementConfigurator c : RootElementConfigurator.all()) {
            elements.add(c);
            listElements(elements, c.describe());
        }
        return elements;
    }

    // for documentation generation in index.jelly
    public List<?> getRootConfigurators() {
        return RootElementConfigurator.all();
    }

    private void listElements(List<Object> elements, Set<Attribute> attributes) {
        for (Attribute attribute : attributes) {

            final Class type = attribute.type;
            Configurator configurator = Configurator.lookup(type);
            if (configurator == null ) {
                continue;
            }
            for (Object o : configurator.getConfigurators()) {
                if (!elements.contains(o)) {
                    elements.add(o);
                }
            }
            listElements(elements, configurator.describe());
        }
    }

    public Map<String, InputStream> getConfigurationInputs(String configPath) throws IOException {
        //Default
        if(StringUtils.isBlank(configPath)) {
            File defaultConfig = new File("./jenkins.yaml");
            if(defaultConfig.exists()) {
                return Collections.singletonMap("jenkins.yaml", new FileInputStream(new File("./jenkins.yaml")));
            } else {
                return Collections.EMPTY_MAP;
            }
        }
        File cfg = new File(configPath);
        if(cfg.isDirectory()) {
            Map<String, InputStream> is = new HashMap<>();
            for(File cfgFile : FileUtils.listFiles(cfg, new String[]{"yml","yaml"},true)) {
                is.put(cfgFile.getName(), new FileInputStream(cfgFile));
            }
            return is;
        }
        return Collections.singletonMap(cfg.getName(), new FileInputStream(cfg));
    }

}
