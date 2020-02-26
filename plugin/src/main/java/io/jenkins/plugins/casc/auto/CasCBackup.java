package io.jenkins.plugins.casc.auto;

import hudson.Extension;
import hudson.XmlFile;
import hudson.init.InitMilestone;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;
import io.jenkins.plugins.casc.ConfigurationAsCode;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;

@Extension(ordinal = 100)
public class CasCBackup extends SaveableListener {
    private static final Logger LOGGER = Logger.getLogger(CasCBackup.class.getName());

    private static final String DEFAULT_JENKINS_YAML_PATH = "jenkins.yaml";
    private static final String cascDirectory = "/WEB-INF/" + DEFAULT_JENKINS_YAML_PATH + ".bak/";

    private static final boolean enableBackup;

    static {
        enableBackup = "true".equals(System.getenv("CASC_AUTO_BACKUP"));

        LOGGER.warning("CasCBackup is " + (enableBackup ? "enabled" : "disabled"));
    }

    @Override
    public void onChange(Saveable o, XmlFile file) {
        if (!enableBackup) {
            return;
        }

        InitMilestone initLevel = Jenkins.getInstance().getInitLevel();
        if (initLevel != InitMilestone.COMPLETED) {
            return;
        }

        // only take care of the configuration which controlled by casc
        if (!(o instanceof GlobalConfiguration)) {
            return;
        }

        LOGGER.info("start to backup casc yaml file");

        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try {
            ConfigurationAsCode.get().export(buf);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "error happen when exporting the whole config into a YAML", e);
            return;
        }

        final ServletContext servletContext = Jenkins.getInstance().servletContext;
        try {
            URL bundled = servletContext.getResource("/WEB-INF");
            if (bundled != null) {
                File cascDir = new File(bundled.getFile(), DEFAULT_JENKINS_YAML_PATH + ".bak/");

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
            } else {
                LOGGER.severe(String.format("cannot found dir %s under the web root", cascDirectory));
            }
        } catch (MalformedURLException e) {
            LOGGER.log(Level.WARNING, String.format("error happen when finding %s", cascDirectory), e);
        }
    }
}
