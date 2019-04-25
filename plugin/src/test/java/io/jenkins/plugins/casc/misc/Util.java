package io.jenkins.plugins.casc.misc;

import hudson.ExtensionList;
import io.jenkins.plugins.casc.ConfigurationAsCode;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.core.JenkinsConfigurator;
import io.jenkins.plugins.casc.impl.configurators.GlobalConfigurationCategoryConfigurator;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import io.jenkins.plugins.casc.snakeyaml.nodes.Node;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Objects;
import jenkins.model.GlobalConfigurationCategory;
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

    public static Mapping getUnclassifiedRoot(ConfigurationContext context)
            throws Exception {
        GlobalConfigurationCategory.Unclassified unclassified = ExtensionList.lookup(GlobalConfigurationCategory.Unclassified.class).get(0);

        GlobalConfigurationCategoryConfigurator unclassifiedConfigurator = new GlobalConfigurationCategoryConfigurator(unclassified);
        return Objects.requireNonNull(unclassifiedConfigurator.describe(unclassifiedConfigurator.getTargetComponent(context), context)).asMapping();
    }

    public static Mapping getSecurityRoot(ConfigurationContext context)
            throws Exception {
        GlobalConfigurationCategory.Security security = ExtensionList.lookup(GlobalConfigurationCategory.Security.class).get(0);

        GlobalConfigurationCategoryConfigurator securityConfigurator = new GlobalConfigurationCategoryConfigurator(security);
        return Objects.requireNonNull(securityConfigurator.describe(securityConfigurator.getTargetComponent(context), context)).asMapping();
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
