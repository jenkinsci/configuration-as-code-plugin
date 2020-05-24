package io.jenkins.plugins.casc.yaml;

import io.jenkins.plugins.casc.ConfigurationAsCode;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.model.Mapping;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.composer.Composer;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeId;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.parser.ParserImpl;
import org.yaml.snakeyaml.resolver.Resolver;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */

public final class YamlUtils {

    public static final Logger LOGGER = Logger.getLogger(ConfigurationAsCode.class.getName());

    public static Node merge(List<YamlSource> configs,
        ConfigurationContext context) throws ConfiguratorException {
        Node root = null;
        for (YamlSource source : configs) {
            try (Reader r = source.read()) {

                final Node node = read(source, context);

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

    public static Node read(YamlSource source, ConfigurationContext context) throws IOException {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setMaxAliasesForCollections(context.getYamlMaxAliasesForCollections());
        Composer composer = new Composer(
            new ParserImpl(new StreamReaderWithSource(source)),
            new Resolver(),
            loaderOptions);
        try {
            return composer.getSingleNode();
        } catch (YAMLException e) {
            if (e.getMessage().startsWith("Number of aliases for non-scalar nodes exceeds the specified max")) {
                throw new ConfiguratorException(String.format(
                    "%s%nYou can increase the maximum by setting an environment variable or property%n  ENV: %s=\"100\"%n  PROPERTY: -D%s=\"100\"",
                    e.getMessage(), ConfigurationContext.CASC_YAML_MAX_ALIASES_ENV,
                    ConfigurationContext.CASC_YAML_MAX_ALIASES_PROPERTY));
            }
            throw e;
        }
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
    public static Mapping loadFrom(List<YamlSource> sources,
        ConfigurationContext context) throws ConfiguratorException {
        if (sources.isEmpty()) return Mapping.EMPTY;
        final Node merged = merge(sources, context);
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
