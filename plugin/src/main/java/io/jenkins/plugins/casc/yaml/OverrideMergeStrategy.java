package io.jenkins.plugins.casc.yaml;

import hudson.Extension;
import io.jenkins.plugins.casc.ConfiguratorConflictException;
import io.jenkins.plugins.casc.ConfiguratorException;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeId;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

/**
 * Override the configuration by loading order
 */
@Extension
public class OverrideMergeStrategy implements MergeStrategy {

    @Override
    public void merge(Node root, Node node, String source) throws ConfiguratorException {
        if (root.getNodeId() != node.getNodeId()) {
            // means one of those yaml file doesn't conform to JCasC schema
            throw new ConfiguratorException(
                String.format("Found incompatible configuration elements %s %s", source, node.getStartMark()));
        }

        switch (root.getNodeId()) {
            case sequence:
                SequenceNode seq = (SequenceNode) root;
                SequenceNode seq2 = (SequenceNode) node;
                seq.getValue().addAll(seq2.getValue());
                return;
            case mapping:
                MappingNode map = (MappingNode) root;
                MappingNode map2 = (MappingNode) node;
                // merge common entries
                for (int i = 0; i < map2.getValue().size();) {
                    NodeTuple t2 = map2.getValue().get(i);
                    for (NodeTuple tuple : map.getValue()) {

                        final Node key = tuple.getKeyNode();
                        final Node key2 = t2.getKeyNode();
                        if (key.getNodeId() == NodeId.scalar) {
                            // We dont support merge for more complex cases (yet)
                            if (((ScalarNode) key).getValue()
                                .equals(((ScalarNode) key2).getValue())) {
                                try {
                                    merge(tuple.getValueNode(), t2.getValueNode(), source);
                                } catch (ConfiguratorConflictException e) {
                                    map.getValue().set(i, t2);
                                }
                                map2.getValue().remove(i);
                            } else {
                                i++;
                            }
                        } else {
                            throw new ConfiguratorException(
                                String.format("Found non-mergeable configuration keys %s %s)", source,
                                    node.getEndMark()));
                        }
                    }
                }
                // .. and add others
                map.getValue().addAll(map2.getValue());
                return;
            default:
                throw new ConfiguratorConflictException(
                    String.format("Found conflicting configuration at %s %s", source,
                        node.getStartMark()));
        }
    }

    @Override
    public String getName() {
        return "override";
    }
}
