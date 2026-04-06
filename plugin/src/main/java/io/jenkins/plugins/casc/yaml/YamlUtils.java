package io.jenkins.plugins.casc.yaml;

import static java.nio.charset.StandardCharsets.UTF_8;

import io.jenkins.plugins.casc.ConfigurationAsCode;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.model.Mapping;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.composer.Composer;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.parser.ParserImpl;
import org.yaml.snakeyaml.reader.StreamReader;
import org.yaml.snakeyaml.resolver.Resolver;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public final class YamlUtils {

    public static final Logger LOGGER = Logger.getLogger(ConfigurationAsCode.class.getName());

    public static Node merge(List<YamlSource> sources, ConfigurationContext context) throws ConfiguratorException {
        Node root = null;
        MergeStrategy mergeStrategy = MergeStrategyFactory.getMergeStrategyOrDefault(context.getMergeStrategy());
        Map<String, Node> parsedCache = new HashMap<>();

        for (YamlSource<?> source : sources) {
            try (Reader reader = reader(source)) {
                Node node = read(source, reader, context);
                if (node != null) {
                    Set<String> visited = new HashSet<>();
                    visited.add(getCanonicalId(source));
                    node = resolveExtends(node, source, context, visited, parsedCache);
                }

                if (root == null) {
                    root = node;
                } else {
                    if (node != null) {
                        mergeStrategy.merge(root, node, source.toString());
                    }
                }
            } catch (IOException io) {
                throw new ConfiguratorException("Failed to read " + source, io);
            }
        }

        return root;
    }

    public static Node read(YamlSource source, Reader reader, ConfigurationContext context) throws IOException {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setCodePointLimit(context.getYamlCodePointLimit());
        loaderOptions.setMaxAliasesForCollections(context.getYamlMaxAliasesForCollections());
        Composer composer = new Composer(
                new ParserImpl(new StreamReaderWithSource(source, reader), loaderOptions),
                new Resolver(),
                loaderOptions);
        try {
            return composer.getSingleNode();
        } catch (YAMLException e) {
            if (e.getMessage().startsWith("Number of aliases for non-scalar nodes exceeds the specified max")) {
                throw new ConfiguratorException(String.format(
                        "%s%nYou can increase the maximum by setting an environment variable or property%n  ENV: %s=\"100\"%n  PROPERTY: -D%s=\"100\"",
                        e.getMessage(),
                        ConfigurationContext.CASC_YAML_MAX_ALIASES_ENV,
                        ConfigurationContext.CASC_YAML_MAX_ALIASES_PROPERTY));
            }
            throw e;
        }
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

    /**
     * Load configuration-as-code model from a set of Yaml sources, merging documents
     */
    public static Mapping loadFrom(List<YamlSource> sources, ConfigurationContext context)
            throws ConfiguratorException {
        if (sources.isEmpty()) {
            return Mapping.EMPTY;
        }
        final Node merged = merge(sources, context);
        if (merged == null) {
            LOGGER.warning("configuration-as-code yaml source returned an empty document.");
            return Mapping.EMPTY;
        }
        return loadFrom(merged, context);
    }

    /**
     * Load configuration-as-code model from a snakeyaml Node
     */
    private static Mapping loadFrom(Node node, ConfigurationContext context) {
        final LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setMaxAliasesForCollections(context.getYamlMaxAliasesForCollections());
        loaderOptions.setCodePointLimit(context.getYamlCodePointLimit());
        final ModelConstructor constructor = new ModelConstructor(loaderOptions);
        constructor.setComposer(
                new Composer(
                        new ParserImpl(new StreamReader(Reader.nullReader()), loaderOptions),
                        new Resolver(),
                        loaderOptions) {

                    @Override
                    public Node getSingleNode() {
                        return node;
                    }
                });
        return (Mapping) constructor.getSingleData(Mapping.class);
    }

    private static Node resolveExtends(
            Node node,
            YamlSource<?> currentSource,
            ConfigurationContext context,
            Set<String> visited,
            Map<String, Node> parsedCache)
            throws ConfiguratorException {

        if (node instanceof MappingNode mapNode) {
            List<NodeTuple> originalTuples = mapNode.getValue();
            List<NodeTuple> resolvedTuples = new ArrayList<>();

            List<String> extendsPaths = new ArrayList<>();
            boolean hasChanges = false;

            for (NodeTuple tuple : originalTuples) {
                Node keyNode = tuple.getKeyNode();
                Node valueNode = tuple.getValueNode();

                if (keyNode instanceof ScalarNode key && "_extends".equals(key.getValue())) {
                    if (valueNode instanceof ScalarNode scalar) {
                        if (scalar.getValue() == null
                                || scalar.getValue().trim().isEmpty()) {
                            throw new ConfiguratorException("The '_extends' property cannot be empty.");
                        }
                        extendsPaths.add(scalar.getValue());
                    } else if (valueNode instanceof SequenceNode seq) {
                        if (seq.getValue().isEmpty()) {
                            throw new ConfiguratorException("The '_extends' list cannot be empty.");
                        }
                        for (Node item : seq.getValue()) {
                            if (item instanceof ScalarNode scalarItem) {
                                String path = scalarItem.getValue();
                                if (path == null || path.trim().isEmpty()) {
                                    throw new ConfiguratorException(
                                            "Items in the '_extends' list cannot be null or empty strings.");
                                }
                                extendsPaths.add(path);
                            } else {
                                throw new ConfiguratorException(String.format(
                                        "Invalid item in '_extends': expected string but got %s in %s",
                                        item.getNodeId(), currentSource));
                            }
                        }
                    } else {
                        throw new ConfiguratorException(String.format(
                                "Invalid value for '_extends' key. Expected string or list of strings, but found: %s",
                                valueNode.getNodeId()));
                    }
                    hasChanges = true;
                    continue;
                }

                Node resolvedValue =
                        resolveExtends(valueNode, currentSource, context, Set.copyOf(visited), parsedCache);

                if (resolvedValue != valueNode) {
                    resolvedTuples.add(new NodeTuple(keyNode, resolvedValue));
                    hasChanges = true;
                } else {
                    resolvedTuples.add(tuple);
                }
            }

            if (!hasChanges) {
                return node;
            }

            MappingNode newMapNode = new MappingNode(
                    mapNode.getTag(),
                    true,
                    resolvedTuples,
                    mapNode.getStartMark(),
                    mapNode.getEndMark(),
                    mapNode.getFlowStyle());

            if (!extendsPaths.isEmpty()) {
                Node baseNode = null;

                for (String path : extendsPaths) {
                    YamlSource<?> parentSource = resolveRelativeSource(currentSource, path);
                    String parentId = getCanonicalId(parentSource);

                    if (visited.contains(parentId)) {
                        throw new ConfiguratorException("Circular _extends dependency detected: " + parentId);
                    }

                    Node parentNode;
                    if (parsedCache.containsKey(parentId)) {
                        parentNode = cloneNode(parsedCache.get(parentId));
                    } else {
                        Set<String> newVisited = new HashSet<>(visited);
                        newVisited.add(parentId);

                        try (Reader parentReader = reader(parentSource)) {
                            parentNode = read(parentSource, parentReader, context);
                        } catch (IOException | YAMLException e) {
                            throw new ConfiguratorException("Failed to read extended config: " + path, e);
                        }

                        parentNode =
                                resolveExtends(parentNode, parentSource, context, Set.copyOf(newVisited), parsedCache);

                        parentNode = cloneNode(parentNode);
                        parsedCache.put(parentId, parentNode);
                    }

                    if (baseNode == null) {
                        baseNode = parentNode;
                    } else {
                        baseNode = deepMergeNodes(baseNode, parentNode);
                    }
                }

                return deepMergeNodes(baseNode, newMapNode);
            }

            return newMapNode;

        } else if (node instanceof SequenceNode seqNode) {
            List<Node> originalChildren = seqNode.getValue();
            List<Node> resolvedChildren = new ArrayList<>();
            boolean hasChanges = false;

            for (Node child : originalChildren) {
                Node resolvedChild = resolveExtends(child, currentSource, context, Set.copyOf(visited), parsedCache);
                resolvedChildren.add(resolvedChild);
                if (resolvedChild != child) {
                    hasChanges = true;
                }
            }

            if (hasChanges) {
                return new SequenceNode(
                        seqNode.getTag(),
                        true,
                        resolvedChildren,
                        seqNode.getStartMark(),
                        seqNode.getEndMark(),
                        seqNode.getFlowStyle());
            }
        }

        return node;
    }

    private static YamlSource<?> resolveRelativeSource(YamlSource<?> currentSource, String extendsPath)
            throws ConfiguratorException {
        if (ConfigurationAsCode.isSupportedURI(extendsPath)) {
            return YamlSource.of(extendsPath);
        }

        Object src = currentSource.source;

        if (src instanceof Path currentPath) {
            Path resolvedPath = currentPath.resolveSibling(extendsPath).normalize();

            if (!Files.exists(resolvedPath)) {
                throw new ConfiguratorException("Extended configuration file does not exist: " + resolvedPath);
            }
            return YamlSource.of(resolvedPath);

        } else if (src instanceof String) {
            try {
                URI currentUri = new URI((String) src);
                URI resolvedUri = currentUri.resolve(extendsPath).normalize();
                return YamlSource.of(resolvedUri.toString());
            } catch (URISyntaxException e) {
                throw new ConfiguratorException("Invalid base URI to resolve against: " + src, e);
            }

        } else if (src instanceof HttpServletRequest || src instanceof InputStream) {
            throw new ConfiguratorException(
                    "Relative `_extends` paths ('" + extendsPath + "') are not supported for inline configurations. "
                            + "Use an absolute file: or http(s): URL instead.");
        }

        throw new ConfiguratorException("Cannot resolve relative path '" + extendsPath + "' for source type: "
                + src.getClass().getSimpleName());
    }

    private static String extractKey(Node keyNode) throws ConfiguratorException {
        if (keyNode instanceof ScalarNode scalarKey) {
            return scalarKey.getValue();
        }
        throw new ConfiguratorException(String.format(
                "Invalid YAML key type: %s. JCasC only supports scalar (string) keys.", keyNode.getNodeId()));
    }

    private static Node deepMergeNodes(Node base, Node override) throws ConfiguratorException {
        if (base != null && override != null) {
            boolean isBaseMap = base instanceof MappingNode;
            boolean isBaseSeq = base instanceof SequenceNode;
            boolean isOverrideMap = override instanceof MappingNode;
            boolean isOverrideSeq = override instanceof SequenceNode;

            if ((isBaseMap && isOverrideSeq) || (isBaseSeq && isOverrideMap)) {
                throw new ConfiguratorException(String.format(
                        "Type mismatch during merge: Cannot merge a %s and a %s. "
                                + "Check your '_extends' hierarchy for incompatible data structures.",
                        override.getNodeId(), base.getNodeId()));
            }
        }
        if (base instanceof MappingNode baseMap && override instanceof MappingNode overrideMap) {
            Map<String, NodeTuple> mergedTuples = new LinkedHashMap<>();

            for (NodeTuple bTuple : baseMap.getValue()) {
                String key = extractKey(bTuple.getKeyNode());
                mergedTuples.put(key, bTuple);
            }

            for (NodeTuple oTuple : overrideMap.getValue()) {
                String key = extractKey(oTuple.getKeyNode());

                if (mergedTuples.containsKey(key)) {
                    Node bValue = mergedTuples.get(key).getValueNode();
                    Node oValue = oTuple.getValueNode();

                    if (bValue instanceof MappingNode && oValue instanceof MappingNode) {
                        Node mergedValue = deepMergeNodes(bValue, oValue);
                        mergedTuples.put(key, new NodeTuple(oTuple.getKeyNode(), mergedValue));
                    } else {
                        mergedTuples.put(key, new NodeTuple(oTuple.getKeyNode(), cloneNode(oValue)));
                    }
                } else {
                    mergedTuples.put(key, new NodeTuple(oTuple.getKeyNode(), cloneNode(oTuple.getValueNode())));
                }
            }

            return new MappingNode(
                    overrideMap.getTag(),
                    true,
                    new ArrayList<>(mergedTuples.values()),
                    baseMap.getStartMark(),
                    overrideMap.getEndMark(),
                    overrideMap.getFlowStyle());
        }
        return cloneNode(override);
    }

    private static Node cloneNode(Node node) {
        if (node == null) {
            return null;
        }

        if (node instanceof MappingNode mapNode) {
            List<NodeTuple> clonedTuples = new ArrayList<>();
            for (NodeTuple tuple : mapNode.getValue()) {
                clonedTuples.add(new NodeTuple(cloneNode(tuple.getKeyNode()), cloneNode(tuple.getValueNode())));
            }
            return new MappingNode(
                    mapNode.getTag(),
                    true,
                    clonedTuples,
                    mapNode.getStartMark(),
                    mapNode.getEndMark(),
                    mapNode.getFlowStyle());

        } else if (node instanceof SequenceNode seqNode) {
            List<Node> clonedChildren = new ArrayList<>();
            for (Node child : seqNode.getValue()) {
                clonedChildren.add(cloneNode(child));
            }
            return new SequenceNode(
                    seqNode.getTag(),
                    true,
                    clonedChildren,
                    seqNode.getStartMark(),
                    seqNode.getEndMark(),
                    seqNode.getFlowStyle());
        }

        if (node instanceof ScalarNode scalarNode) {
            return new ScalarNode(
                    scalarNode.getTag(),
                    scalarNode.getValue(),
                    scalarNode.getStartMark(),
                    scalarNode.getEndMark(),
                    scalarNode.getScalarStyle());
        }

        return node;
    }

    private static String getCanonicalId(YamlSource<?> source) {
        Object src = source.source;

        if (src instanceof Path path) {
            try {
                return path.toRealPath().toString();
            } catch (IOException e) {
                return path.toAbsolutePath().normalize().toString();
            }
        } else if (src instanceof String url) {
            try {
                return new URI(url).normalize().toString();
            } catch (URISyntaxException e) {
                return url;
            }
        }

        return source.toString();
    }
}
