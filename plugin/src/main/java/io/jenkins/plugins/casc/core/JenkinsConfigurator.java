package io.jenkins.plugins.casc.core;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.ProxyConfiguration;
import hudson.model.Node;
import hudson.model.UpdateCenter;
import hudson.slaves.AbstractCloudSlave;
import hudson.slaves.EphemeralNode;
import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.BaseConfigurator;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.RootElementConfigurator;
import io.jenkins.plugins.casc.model.Mapping;
import io.vavr.control.Try;
import jenkins.model.Jenkins;
import jenkins.security.s2m.AdminWhitelistRule;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static io.jenkins.plugins.casc.Attribute.noop;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension
@Restricted(NoExternalUse.class)
public class JenkinsConfigurator extends BaseConfigurator<Jenkins> implements RootElementConfigurator<Jenkins> {

    @Override
    public Class<Jenkins> getTarget() {
        return Jenkins.class;
    }

    @Override
    public Jenkins getTargetComponent(ConfigurationContext context) {
        return Jenkins.getInstance();
    }

    @Override
    protected Jenkins instance(Mapping mapping, ConfigurationContext context) {
        return getTargetComponent(context);
    }

    @NonNull
    @Override
    public Set<Attribute<Jenkins,?>> describe() {
        final Set<Attribute<Jenkins,?>> attributes = super.describe();

        // Add remoting security, all legwork will be done by a configurator
        attributes.add(new Attribute<Jenkins, AdminWhitelistRule>("remotingSecurity", AdminWhitelistRule.class)
                .getter( j -> j.getInjector().getInstance(AdminWhitelistRule.class) )
                .setter( noop() ));

        // Override "nodes" getter so we don't export Nodes registered by Cloud plugins
        Attribute.<Jenkins, List<Node>>get(attributes, "nodes").ifPresent(attribute ->
                attribute
                        .getter(jenkins -> jenkins.getNodes().stream()
                                .filter(node -> !isCloudNode(node))
                                .collect(Collectors.toList()))
                        .setter((jenkins, configuredNodes) -> {
                            List<String> configuredNodesNames = configuredNodes.stream()
                                    .map(Node::getNodeName)
                                    .collect(Collectors.toList());
                            List<Node> nodesToKeep = jenkins.getNodes().stream()
                                    .filter(node -> !configuredNodesNames.contains(node.getNodeName()))
                                    .filter(this::isCloudNode)
                                    .collect(Collectors.toList());
                            nodesToKeep.addAll(configuredNodes);
                            jenkins.setNodes(nodesToKeep);
                        })
        );

        // Add updateCenter, all legwork will be done by a configurator
        attributes.add(new Attribute<Jenkins, UpdateCenter>("updateCenter", UpdateCenter.class)
                .getter(Jenkins::getUpdateCenter)
                .setter( noop() ));

        attributes.add(new Attribute<Jenkins, ProxyConfiguration>("proxy", ProxyConfiguration.class)
                .getter( j -> j.proxy)
                .setter((o, v) -> o.proxy = v));

        return attributes;
    }


    private boolean isCloudNode(Node node) {
        boolean instantiable = Try.of(() -> node.getDescriptor().isInstantiable()).getOrElse(true);
        final boolean cloudSlave = node instanceof AbstractCloudSlave;
        final boolean ephemeral = node instanceof EphemeralNode;
        return !instantiable || cloudSlave || ephemeral;
    }
    
    @Override
    protected Set<String> exclusions() {
        return Collections.singleton("installState");
    }

    @NonNull
    @Override
    public String getName() {
        return "jenkins";
    }
}
