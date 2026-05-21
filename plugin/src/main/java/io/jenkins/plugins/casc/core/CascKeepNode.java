package io.jenkins.plugins.casc.core;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Node;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Marker {@link NodeProperty} that instructs the JCasc reload to preserve this node.
 *
 * <p>During a {@code jenkins.yaml} reload, JCasc reconciles the {@code nodes:} list by removing
 * every node that is <em>not</em> listed in the YAML. Nodes created dynamically at runtime (e.g.
 * by aquarium-gateway) are not in the YAML and would normally be deleted.
 *
 * <p>Attaching this property to a node signals {@link JenkinsConfigurator#shouldKeepNode} to treat it
 * like a cloud-provisioned node and skip it during reconciliation, so the node survives reloads
 * without needing to implement {@link hudson.slaves.EphemeralNode} or extend
 * {@link hudson.slaves.AbstractCloudSlave}.
 */
public class CascKeepNode extends NodeProperty<Node> {

    @DataBoundConstructor
    public CascKeepNode() {}

    @Extension
    public static final class DescriptorImpl extends NodePropertyDescriptor {

        @NonNull
        @Override
        public String getDisplayName() {
            return "Keep node during Configuration as Code reload";
        }
    }
}
