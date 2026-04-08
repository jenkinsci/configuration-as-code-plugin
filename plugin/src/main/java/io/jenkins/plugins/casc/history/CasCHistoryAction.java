package io.jenkins.plugins.casc.history;

import hudson.Extension;
import hudson.model.ManagementLink;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;
import org.kohsuke.stapler.verb.GET;
import org.springframework.lang.NonNull;
import org.w3c.dom.Document;

@Extension
public class CasCHistoryAction extends ManagementLink {

    private static final Logger LOGGER = Logger.getLogger(CasCHistoryAction.class.getName());

    @Override
    public String getIconFileName() {
        return "symbol-time";
    }

    @Override
    public String getDisplayName() {
        return "CasC History";
    }

    @Override
    public String getUrlName() {
        return "casc-history";
    }

    @Override
    public String getDescription() {
        return "Browse CasC history and snapshots.";
    }

    public List<HistoryEntry> getHistoryEntries() {
        List<HistoryEntry> entries = new ArrayList<>();
        File baseDir = new File(Jenkins.get().getRootDir(), "casc-history");

        if (baseDir.exists() && baseDir.isDirectory()) {
            File[] historyFolders = baseDir.listFiles(File::isDirectory);
            if (historyFolders != null) {
                Arrays.sort(historyFolders, Comparator.comparing(File::getName).reversed());

                for (File folder : historyFolders) {
                    entries.add(new HistoryEntry(folder.getName(), parseUserFromXml(folder)));
                }
            }
        }
        return entries;
    }

    private String parseUserFromXml(File folder) {
        File xmlFile = new File(folder, "history.xml");
        if (xmlFile.exists()) {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(xmlFile);

                return doc.getElementsByTagName("user").item(0).getTextContent();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to read history.xml in " + folder.getName(), e);
            }
        }
        return "Unknown User";
    }

    public record HistoryEntry(String timestamp, String user) {

        @SuppressWarnings("unused")
        public String getFormattedTimestamp() {
            LocalDateTime dt = LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

            return dt.format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss"));
        }
    }

    @GET
    @SuppressWarnings("unused")
    public void doView(StaplerRequest2 req, StaplerResponse2 res) throws IOException {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);

        String timestamp = req.getParameter("timestamp");
        if (timestamp == null || timestamp.isEmpty()) {
            res.sendError(400, "Missing timestamp parameter");
            return;
        }

        if (!timestamp.matches("^[0-9]{4}-[0-9]{2}-[0-9]{2}_[0-9]{2}-[0-9]{2}-[0-9]{2}(_[0-9]+)?$")) {
            res.sendError(400, "Invalid timestamp format");
            return;
        }

        File baseDir = new File(Jenkins.get().getRootDir(), "casc-history");
        File historyDir = new File(baseDir, timestamp);
        File yamlFile = new File(historyDir, "jenkins.yaml");

        if (!yamlFile.exists()) {
            res.sendError(404, "History record not found");
            return;
        }

        res.setContentType("text/plain;charset=UTF-8");
        Files.copy(yamlFile.toPath(), res.getOutputStream());
    }

    @Override
    @NonNull
    public Category getCategory() {
        return Category.CONFIGURATION;
    }
}
