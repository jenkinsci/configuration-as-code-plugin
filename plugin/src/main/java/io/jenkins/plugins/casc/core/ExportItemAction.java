package io.jenkins.plugins.casc.core;

import hudson.model.Action;
import hudson.model.TopLevelItem;
import io.jenkins.plugins.casc.ConfigurationAsCode;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.model.CNode;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.kohsuke.stapler.StaplerResponse2;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.yaml.snakeyaml.nodes.Node;

@SuppressWarnings("ClassCanBeRecord")
public class ExportItemAction implements Action {

    private static final Logger LOGGER = Logger.getLogger(ExportItemAction.class.getName());
    private final TopLevelItem item;

    public ExportItemAction(TopLevelItem item) {
        this.item = item;
    }

    @Override
    public String getIconFileName() {
        return item.hasPermission(TopLevelItem.CONFIGURE) ? "symbol-code" : null;
    }

    @Override
    public String getDisplayName() {
        return "Export";
    }

    @Override
    public String getUrlName() {
        return "jcasc-export";
    }

    public TopLevelItem getItem() {
        return item;
    }

    public String getConfig() {
        item.checkPermission(TopLevelItem.CONFIGURE);

        try {
            ConfigurationContext context = new ConfigurationContext(ConfiguratorRegistry.get());
            ItemExporter exporter = new ItemExporter();
            CNode rootNode = exporter.export(item, context);

            if (rootNode == null) {
                return "# No JCasC configurator found for this item type.";
            }

            ConfigurationAsCode casc = ConfigurationAsCode.get();
            Node yamlNode = casc.toYaml(rootNode);

            if (yamlNode != null) {
                StringWriter writer = new StringWriter();
                ConfigurationAsCode.serializeYamlNode(yamlNode, writer);
                return writer.toString();
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to export JCasC for item: " + item.getFullName(), e);
            return "# Error exporting configuration: " + e.getMessage();
        }

        return "# Could not generate JCasC configuration.";
    }

    @RequirePOST
    @SuppressWarnings("unused")
    public void doDownloadYaml(StaplerResponse2 rsp) throws Exception {
        item.checkPermission(TopLevelItem.CONFIGURE);

        String yamlConfig = getConfig();

        rsp.setContentType("application/x-yaml;charset=UTF-8");

        String safeName = item.getName().replaceAll("[^a-zA-Z0-9_-]", "_").toLowerCase();
        rsp.setHeader("Content-Disposition", "attachment; filename=" + safeName + "-jcasc.yaml");

        PrintWriter writer = rsp.getWriter();
        writer.print(yamlConfig);
        writer.flush();
    }
}
