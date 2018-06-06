package org.jenkinsci.plugins.casc.yaml;

import org.jenkinsci.plugins.casc.ConfiguratorException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.composer.Composer;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeId;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.parser.ParserImpl;
import org.yaml.snakeyaml.reader.StreamReader;
import org.yaml.snakeyaml.resolver.Resolver;

import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.List;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public final class YamlUtils {

    public static Node merge(List<YamlSource> configs) throws ConfiguratorException {
        Node root = null;
        for (YamlSource source : configs) {
            try (Reader r = source.read()) {

                Composer composer = new Composer(new ParserImpl(new StreamReaderWithSource(source)), new Resolver());
                final Node node =  composer.getSingleNode();

                if (root == null) {
                    root = node;
                } else {
                    merge(root, node, source.toString());
                }
            } catch (IOException io) {
                throw new ConfiguratorException("Failed to read " + source, io);
            }
        }

        return root;
    }

    private static void merge(Node root, Node node, String source) throws ConfiguratorException {
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
                final Iterator<NodeTuple> it = map2.getValue().iterator();
                while (it.hasNext()) {
                    NodeTuple t2 = it.next();
                    for (NodeTuple tuple : map.getValue()) {

                        final Node key = tuple.getKeyNode();
                        final Node key2 = t2.getKeyNode();
                        if (key.getNodeId() == NodeId.scalar) {
                            // We dont support merge for more complex cases (yet)
                            if (((ScalarNode) key).getValue().equals(((ScalarNode) key2).getValue())) {
                                merge(tuple.getValueNode(), t2.getValueNode(), source);
                                it.remove();
                            }
                        } else {
                            throw new ConfiguratorException(
                                    String.format("Found unmergeable configuration keys %s %s)", source, node.getEndMark()));
                        }
                    }
                }
                // .. and add others
                map.getValue().addAll(map2.getValue());
                return;
            default:
                throw new ConfiguratorException(
                        String.format("Found conflicting configuration at %s %s", source.toString(), node.getStartMark()));
        }

    }
}
