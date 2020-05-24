package io.jenkins.plugins.casc.yaml;

import io.jenkins.plugins.casc.ConfigurationAsCode;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.model.Mapping;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.yaml.snakeyaml.composer.Composer;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeId;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.parser.ParserImpl;
import org.yaml.snakeyaml.resolver.Resolver;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */

public final class YamlUtils {

    public static final Logger LOGGER = Logger.getLogger(ConfigurationAsCode.class.getName());

    public static Node merge(List<YamlSource> sources) throws ConfiguratorException {
        Node root = null;
        for (YamlSource<?> source : sources) {
            try (Reader reader = reader(source)) {
                final Node node = read(source, reader);

                if (root == null) {
                    root = node;
                } else {
                    if (node != null) {
                        merge(root, node, source.toString());
                    }
                }
            } catch (IOException io) {
                throw new ConfiguratorException("Failed to read " + source, io);
            }
        }

        return root;
    }

    public static Node read(YamlSource source, Reader reader) throws IOException {
        Composer composer = new Composer(new ParserImpl(new StreamReaderWithSource(source, reader)), new Resolver());
        return composer.getSingleNode();
    }

    public static Reader reader(YamlSource<?> source) throws IOException {
        Object src = source.source;
        if (src instanceof String) {
            final URL url = URI.create((String) src).toURL();
            return new InputStreamReader(url.openStream(), UTF_8);
        } else if (src instanceof InputStream) {
            return new InputStreamReader((InputStream) src, UTF_8);
        } else if (src instanceof HttpServletRequest) {
            return new InputStreamReader(((HttpServletRequest) src).getInputStream(), UTF_8);
        } else if (src instanceof Path) {
            return Files.newBufferedReader((Path) src);
        }
        throw new IOException(String.format("Unknown %s", source));
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
                        String.format("Found conflicting configuration at %s %s", source, node.getStartMark()));
        }

    }

    /**
     * Load configuration-as-code model from a set of Yaml sources, merging documents
     */
    public static Mapping loadFrom(List<YamlSource> sources) throws ConfiguratorException {
        if (sources.isEmpty()) return Mapping.EMPTY;
        final Node merged = merge(sources);
        if (merged == null) {
            LOGGER.warning("configuration-as-code yaml source returned an empty document.");
            return Mapping.EMPTY;
        }
        return loadFrom(merged);
    }

    /**
     * Load configuration-as-code model from a snakeyaml Node
     */
    private static Mapping loadFrom(Node node) {
        final ModelConstructor constructor = new ModelConstructor();
        constructor.setComposer(new Composer(null, null) {

            @Override
            public Node getSingleNode() {
                return node;
            }
        });
        return (Mapping) constructor.getSingleData(Mapping.class);
    }
}
