package io.jenkins.plugins.casc.auto;

import hudson.Extension;
import hudson.XmlFile;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;
import io.jenkins.plugins.casc.ConfigurationAsCode;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.impl.DefaultConfiguratorRegistry;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;

@Extension(ordinal = 100)
public class CasCBackup extends SaveableListener {
    private static final Logger LOGGER = Logger.getLogger(CasCBackup.class.getName());

    private static final String DEFAULT_JENKINS_YAML_PATH = "jenkins.yaml";
    private static final String cascDirectory = "/WEB-INF/" + DEFAULT_JENKINS_YAML_PATH + ".d/";

    @Inject
    private DefaultConfiguratorRegistry registry;

    @Override
    public void onChange(Saveable o, XmlFile file) {
        ConfigurationContext context = new ConfigurationContext(registry);
        if (!context.isEnableBackup()) {
            return;
        }

        // only take care of the configuration which controlled by casc
        if (!(o instanceof GlobalConfiguration)) {
            return;
        }

        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try {
            ConfigurationAsCode.get().export(buf);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "error happen when exporting the whole config into a YAML", e);
            return;
        }

        final ServletContext servletContext = Jenkins.getInstance().servletContext;
        try {
            URL bundled = servletContext.getResource(cascDirectory);
            if (bundled != null) {
                File cascDir = new File(bundled.getFile());

                boolean hasDir = false;
                if(!cascDir.exists()) {
                    hasDir = cascDir.mkdirs();
                } else if (cascDir.isFile()) {
                    LOGGER.severe(String.format("%s is a regular file", cascDir));
                } else {
                    hasDir = true;
                }

                if(hasDir) {
                    File backupFile = new File(cascDir, "user.yaml");
                    try (OutputStream writer = new FileOutputStream(backupFile)) {
                        writer.write(buf.toByteArray());

                        LOGGER.fine(String.format("backup file was saved, %s", backupFile.getAbsolutePath()));
                    } catch (IOException e) {
                        LOGGER.log(Level.WARNING, String.format("error happen when saving %s", backupFile.getAbsolutePath()), e);
                    }
                } else {
                    LOGGER.severe(String.format("cannot create casc backup directory %s", cascDir));
                }
            }
        } catch (MalformedURLException e) {
            LOGGER.log(Level.WARNING, String.format("error happen when finding %s", cascDirectory), e);
        }
    }
}
