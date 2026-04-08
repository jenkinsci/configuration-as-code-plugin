package io.jenkins.plugins.casc.history;

import static org.apache.commons.lang.StringEscapeUtils.escapeXml;

import hudson.Extension;
import hudson.Util;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;

@Extension
public class LocalFileHistoryBackend extends CasCHistoryBackend {
    private static final Logger LOGGER = Logger.getLogger(LocalFileHistoryBackend.class.getName());
    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd_HH-mm-ss";
    private static final int MAX_HISTORY_ENTRIES = 50;
    private final Object writeLock = new Object();

    @Override
    public void save(String yamlContent, String triggeredBy) throws IOException {
        File jenkinsHome = Jenkins.get().getRootDir();
        File baseHistoryDir = new File(jenkinsHome, "casc-history");

        synchronized (writeLock) {
            if (!baseHistoryDir.mkdirs() && !baseHistoryDir.exists()) {
                throw new IOException("Failed to create base history directory: " + baseHistoryDir.getAbsolutePath());
            }

            String timestamp = getCurrentTimestamp();
            File specificHistoryDir = new File(baseHistoryDir, timestamp);

            int counter = 1;
            while (specificHistoryDir.exists()) {
                specificHistoryDir = new File(baseHistoryDir, timestamp + "_" + counter);
                counter++;
            }

            if (!specificHistoryDir.mkdirs()) {
                throw new IOException(
                        "Failed to create specific history directory: " + specificHistoryDir.getAbsolutePath());
            }

            Path yamlFile = new File(specificHistoryDir, "jenkins.yaml").toPath();
            Files.writeString(yamlFile, yamlContent);

            String xmlMetadata = String.format(
                    "<?xml version='1.1' encoding='UTF-8'?>%n" + "<history>%n"
                            + "  <user>%s</user>%n"
                            + "  <timestamp>%s</timestamp>%n"
                            + "</history>",
                    escapeXml(triggeredBy), timestamp);
            Path metadataFile = new File(specificHistoryDir, "history.xml").toPath();
            Files.writeString(metadataFile, xmlMetadata);

            LOGGER.info("CasC history successfully saved at: " + specificHistoryDir.getAbsolutePath());

            cleanupOldHistory(baseHistoryDir);
        }
    }

    private void cleanupOldHistory(File baseHistoryDir) {
        File[] historyFolders = baseHistoryDir.listFiles(File::isDirectory);

        if (historyFolders != null && historyFolders.length > MAX_HISTORY_ENTRIES) {
            Arrays.sort(historyFolders, Comparator.comparing(File::getName));
            int directoriesToDelete = historyFolders.length - MAX_HISTORY_ENTRIES;

            for (int i = 0; i < directoriesToDelete; i++) {
                File folderToDelete = historyFolders[i];
                LOGGER.fine("Deleting old CasC history entry: " + folderToDelete.getName());
                try {
                    Util.deleteRecursive(folderToDelete);
                } catch (IOException e) {
                    LOGGER.log(
                            Level.WARNING,
                            "Failed to delete old CasC history folder: " + folderToDelete.getAbsolutePath(),
                            e);
                }
            }
        }
    }

    String getCurrentTimestamp() {
        return new SimpleDateFormat(TIMESTAMP_FORMAT).format(new Date());
    }
}
