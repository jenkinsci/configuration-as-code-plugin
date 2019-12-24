package io.jenkins.plugins.casc.yaml;

import io.jenkins.plugins.casc.ConfigurationAsCode;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.model.Mapping;
import io.jenkins.plugins.casc.snakeyaml.composer.Composer;
import io.jenkins.plugins.casc.snakeyaml.nodes.Node;
import io.jenkins.plugins.casc.snakeyaml.parser.ParserImpl;
import io.jenkins.plugins.casc.snakeyaml.resolver.Resolver;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */

public final class YamlUtils {

    public static final Logger LOGGER = Logger.getLogger(ConfigurationAsCode.class.getName());

    public static Node merge(List<YamlSource> configs, MergeStrategy mergeStrategy) throws ConfiguratorException {
        Node root = null;
        for (YamlSource source : configs) {
            try (Reader r = source.read()) {

                final Node node = read(source);

                if (root == null) {
                    root = node;
                } else {
                    if (node != null) {
                        root = mergeStrategy.merge(root, node, source.toString());
                    }
                }
            } catch (IOException io) {
                throw new ConfiguratorException("Failed to read " + source, io);
            }
        }

        return root;
    }

    public static Node read(YamlSource source) throws IOException {
        Composer composer = new Composer(new ParserImpl(new StreamReaderWithSource(source)), new Resolver());
        return composer.getSingleNode();
    }

    /**
     * Load configuration-as-code model from a set of Yaml sources, merging documents
     */
    public static Mapping loadFrom(List<YamlSource> sources) throws ConfiguratorException {
        if (sources.isEmpty()) return Mapping.getEmpty();
        final Node merged = merge(sources);
        if (merged == null) {
            LOGGER.warning("configuration-as-code yaml source returned an empty document.");
            return Mapping.getEmpty();
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
