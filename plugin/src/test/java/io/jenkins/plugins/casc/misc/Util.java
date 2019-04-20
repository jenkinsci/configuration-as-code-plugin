package io.jenkins.plugins.casc.misc;

import io.jenkins.plugins.casc.ConfigurationAsCode;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.core.JenkinsConfigurator;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import io.jenkins.plugins.casc.snakeyaml.nodes.Node;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Objects;
import jenkins.model.Jenkins;

import static io.jenkins.plugins.casc.ConfigurationAsCode.serializeYamlNode;

public class Util {
    public static JenkinsConfigurator getJenkinsConfigurator() {
        return Jenkins.getInstance().getExtensionList(JenkinsConfigurator.class).get(0);
    }

    public static Mapping getJenkinsRoot(ConfigurationContext context)
        throws Exception {
        JenkinsConfigurator root = getJenkinsConfigurator();
        return Objects.requireNonNull(root.describe(root.getTargetComponent(context), context)).asMapping();
    }

    public static Mapping getJenkinsRoot(JenkinsConfigurator root, ConfigurationContext context)
        throws Exception {
        return Objects.requireNonNull(root.describe(root.getTargetComponent(context), context)).asMapping();
    }

    public static String toYamlString(CNode rootNode) throws IOException {
        Node yamlRoot = ConfigurationAsCode.get().toYaml(rootNode);
        StringWriter buffer = new StringWriter();
        serializeYamlNode(yamlRoot, buffer);
        return buffer.toString();
    }
}
