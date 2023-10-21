package io.jenkins.plugins.casc.core;

import static io.jenkins.plugins.casc.Attribute.noop;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.ProxyConfiguration;
import hudson.model.ComputerSet;
import hudson.model.Descriptor;
import hudson.model.Node;
import hudson.model.UpdateCenter;
import hudson.model.labels.LabelAtom;
import hudson.node_monitors.NodeMonitor;
import hudson.slaves.AbstractCloudSlave;
import hudson.slaves.EphemeralNode;
import hudson.util.DescribableList;
import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.BaseConfigurator;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.RootElementConfigurator;
import io.jenkins.plugins.casc.impl.attributes.MultivaluedAttribute;
import io.jenkins.plugins.casc.model.Mapping;
import io.vavr.control.Try;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import jenkins.model.Jenkins;
import jenkins.security.s2m.AdminWhitelistRule;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension(ordinal = 1)
@Restricted(NoExternalUse.class)
public class JenkinsConfigurator extends BaseConfigurator<Jenkins> implements RootElementConfigurator<Jenkins> {

    @Override
    public Class<Jenkins> getTarget() {
        return Jenkins.class;
    }

    @Override
    public Jenkins getTargetComponent(ConfigurationContext context) {
        return Jenkins.get();
    }

    @Override
    protected Jenkins instance(Mapping mapping, ConfigurationContext context) {
        return getTargetComponent(context);
    }

    @NonNull
    @Override
    public Set<Attribute<Jenkins, ?>> describe() {
        final Set<Attribute<Jenkins, ?>> attributes = super.describe();

        // Add remoting security, all legwork will be done by a configurator
        attributes.add(new Attribute<Jenkins, AdminWhitelistRule>("remotingSecurity", AdminWhitelistRule.class)
                .getter(j -> j.getInjector().getInstance(AdminWhitelistRule.class))
                .setter(noop()));

        // Override "nodes" getter so we don't export Nodes registered by Cloud plugins
        Attribute.<Jenkins, List<Node>>get(attributes, "nodes").ifPresent(attribute -> attribute
                .getter(jenkins -> jenkins.getNodes().stream()
                        .filter(node -> !isCloudNode(node))
                        .collect(Collectors.toList()))
                .setter((jenkins, configuredNodes) -> {
                    List<String> configuredNodesNames =
                            configuredNodes.stream().map(Node::getNodeName).collect(Collectors.toList());
                    List<Node> nodesToKeep = jenkins.getNodes().stream()
                            .filter(node -> !configuredNodesNames.contains(node.getNodeName()))
                            .filter(this::isCloudNode)
                            .collect(Collectors.toList());
                    nodesToKeep.addAll(configuredNodes);
                    jenkins.setNodes(nodesToKeep);
                }));

        // Add updateCenter, all legwork will be done by a configurator
        attributes.add(new Attribute<Jenkins, UpdateCenter>("updateCenter", UpdateCenter.class)
                .getter(Jenkins::getUpdateCenter)
                .setter(noop()));

        attributes.add(new MultivaluedAttribute<Jenkins, LabelAtom>("labelAtoms", LabelAtom.class)
                .getter(Jenkins::getLabelAtoms)
                .setter((jenkins, labelAtoms) -> {
                    for (LabelAtom labelAtom : labelAtoms) {
                        // Get or create a LabelAtom instance
                        LabelAtom atom = jenkins.getLabelAtom(labelAtom.getName());

                        if (atom != null) {
                            atom.getProperties().clear();
                            atom.getProperties().addAll(labelAtom.getProperties());
                        }
                    }
                }));

        attributes.add(new Attribute<Jenkins, ProxyConfiguration>("proxy", ProxyConfiguration.class)
                .getter(j -> j.proxy)
                .setter((o, v) -> o.proxy = v));

        attributes.add(new MultivaluedAttribute<Jenkins, NodeMonitor>("nodeMonitors", NodeMonitor.class)
            .getter(j -> ComputerSet.getMonitors())
            .setter((jenkins, nodeMonitors) -> {
                DescribableList<NodeMonitor, Descriptor<NodeMonitor>> monitors = ComputerSet.getMonitors();
                monitors.clear();
                monitors.addAll(nodeMonitors);
                for (Descriptor<NodeMonitor> d : NodeMonitor.all())
                    if (monitors.get(d) == null) {
                        NodeMonitor i = createDefaultInstance(d);
                        if (i != null)
                            monitors.add(i);
                    }
            }));

        return attributes;
    }

    private static NodeMonitor createDefaultInstance(Descriptor<NodeMonitor> d) {
        try {
            NodeMonitor nm = d.clazz.getDeclaredConstructor().newInstance();
            nm.setIgnored(true);
            return nm;
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                 InvocationTargetException e) {
            LOGGER.log(Level.SEVERE, "Failed to instantiate " + d.clazz, e);
        }
        return null;
    }

    private boolean isCloudNode(Node node) {
        boolean instantiable =
                Try.of(() -> node.getDescriptor().isInstantiable()).getOrElse(true);
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

    private static final Logger LOGGER = Logger.getLogger(JenkinsConfigurator.class.getName());
}
