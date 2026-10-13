package io.jenkins.plugins.casc.core;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorConflictException;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.impl.configurators.DataBoundConfigurator;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import io.jenkins.plugins.casc.yaml.MergeStrategy;
import io.jenkins.plugins.casc.yaml.MergeStrategyFactory;
import java.util.Iterator;
import java.util.Objects;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

@Extension
public class GlobalNodePropertiesConfigurator extends DataBoundConfigurator<EnvironmentVariablesNodeProperty> {

    public GlobalNodePropertiesConfigurator() {
        this(EnvironmentVariablesNodeProperty.class);
    }

    public GlobalNodePropertiesConfigurator(Class<?> clazz) {
        super(EnvironmentVariablesNodeProperty.class);
    }

    @NonNull
    @Override
    public String getName() {
        return "globalNodeProperties";
    }

    @NonNull
    @Override
    public EnvironmentVariablesNodeProperty configure(CNode c, ConfigurationContext context)
            throws ConfiguratorException {
        return super.configure(c, context);
    }

    @Override
    public void merge(Node root, ConfigurationContext context) throws ConfiguratorException {
        if (!(root instanceof MappingNode rootMap)) {
            return;
        }

        Node jenkinsNode = getMappingValue(rootMap, "jenkins");
        if (!(jenkinsNode instanceof MappingNode jenkinsMap)) {
            return;
        }

        Node propertiesNode = getMappingValue(jenkinsMap, getName());
        if (!(propertiesNode instanceof SequenceNode properties)) {
            return;
        }

        MergeStrategy mergeStrategy =
                MergeStrategyFactory.getMergeStrategyOrDefault(context != null ? context.getMergeStrategy() : null);
        boolean isOverride = "override".equals(mergeStrategy.getName());
        Node mergedEnv = null;

        Iterator<Node> iterator = properties.getValue().iterator();
        while (iterator.hasNext()) {
            Node propertyNode = iterator.next();
            if (!(propertyNode instanceof MappingNode property)) {
                continue;
            }

            if (property.getValue().size() != 1) {
                continue;
            }

            NodeTuple propertyTuple = property.getValue().get(0);
            if (!isKey(propertyTuple.getKeyNode(), "envVars")) {
                continue;
            }

            Node envVarsNode = propertyTuple.getValueNode();
            if (!isCompatibleEnvVars(envVarsNode)) {
                continue;
            }

            Node envNode = getMappingValue((MappingNode) envVarsNode, "env");
            if (envNode == null) {
                continue;
            }

            boolean isValid = (envNode instanceof SequenceNode seq && isStrictlyValidEnvSequence(seq))
                    || (envNode instanceof MappingNode map && isStrictlyValidEnvMapping(map));

            if (mergedEnv == null) {
                if (isValid) {
                    mergedEnv = envNode;
                }
            } else {
                if (!isValid) {
                    continue;
                }

                if (mergedEnv instanceof SequenceNode mergedSeq && envNode instanceof SequenceNode envSeq) {
                    mergeEnvSequence(mergedSeq, envSeq, isOverride);
                    iterator.remove();
                } else if (mergedEnv instanceof MappingNode mergedMap && envNode instanceof MappingNode envMap) {
                    mergeEnvMapping(mergedMap, envMap, isOverride);
                    iterator.remove();
                } else {
                    throw new ConfiguratorException(
                            "Incompatible environment variable definitions: cannot merge a sequence with a mapping.");
                }
            }
        }
    }

    private void mergeEnvSequence(SequenceNode mergedSeq, SequenceNode envSeq, boolean isOverride)
            throws ConfiguratorConflictException {
        for (Node entryNode : envSeq.getValue()) {
            MappingNode entryMap = (MappingNode) entryNode;
            String key = getScalarValue(entryMap, "key");
            String newValue = getScalarValue(entryMap, "value");

            int existingIndex = -1;
            MappingNode existingMap = null;
            for (int i = 0; i < mergedSeq.getValue().size(); i++) {
                Node existing = mergedSeq.getValue().get(i);
                if (existing instanceof MappingNode m) {
                    if (Objects.equals(key, getScalarValue(m, "key"))) {
                        existingIndex = i;
                        existingMap = m;
                        break;
                    }
                }
            }

            if (existingIndex >= 0) {
                String existingValue = getScalarValue(existingMap, "value");
                if (isOverride) {
                    mergedSeq.getValue().set(existingIndex, entryNode);
                } else if (!Objects.equals(existingValue, newValue)) {
                    throw new ConfiguratorConflictException(
                            String.format("Found conflicting environment variable '%s'", key));
                }
            } else {
                mergedSeq.getValue().add(entryNode);
            }
        }
    }

    private void mergeEnvMapping(MappingNode mergedMap, MappingNode envMap, boolean isOverride)
            throws ConfiguratorConflictException {
        for (NodeTuple tuple : envMap.getValue()) {
            ScalarNode keyScalar = (ScalarNode) tuple.getKeyNode();
            ScalarNode valueScalar = (ScalarNode) tuple.getValueNode();

            String key = keyScalar.getValue();
            String newValue = valueScalar.getValue();

            int existingIndex = -1;
            NodeTuple existingTuple = null;
            for (int i = 0; i < mergedMap.getValue().size(); i++) {
                NodeTuple t = mergedMap.getValue().get(i);
                if (t.getKeyNode() instanceof ScalarNode k && key.equals(k.getValue())) {
                    existingIndex = i;
                    existingTuple = t;
                    break;
                }
            }

            if (existingIndex >= 0) {
                String existingValue = ((ScalarNode) existingTuple.getValueNode()).getValue();
                if (isOverride) {
                    mergedMap.getValue().set(existingIndex, tuple);
                } else if (!Objects.equals(existingValue, newValue)) {
                    throw new ConfiguratorConflictException(
                            String.format("Found conflicting environment variable '%s'", key));
                }
            } else {
                mergedMap.getValue().add(tuple);
            }
        }
    }

    private static boolean isStrictlyValidEnvSequence(SequenceNode seq) {
        for (Node entryNode : seq.getValue()) {
            if (!(entryNode instanceof MappingNode entryMap)) {
                return false;
            }
            if (entryMap.getValue().size() != 2) {
                return false;
            }

            boolean hasKey = false;
            boolean hasValue = false;

            for (NodeTuple tuple : entryMap.getValue()) {
                if (!(tuple.getKeyNode() instanceof ScalarNode kNode)) {
                    return false;
                }
                String k = kNode.getValue();
                if ("key".equals(k)) {
                    if (!(tuple.getValueNode() instanceof ScalarNode)) {
                        return false;
                    }
                    hasKey = true;
                } else if ("value".equals(k)) {
                    if (!(tuple.getValueNode() instanceof ScalarNode)) {
                        return false;
                    }
                    hasValue = true;
                } else {
                    return false;
                }
            }

            if (!hasKey || !hasValue) {
                return false;
            }
        }
        return true;
    }

    private static boolean isStrictlyValidEnvMapping(MappingNode map) {
        for (NodeTuple tuple : map.getValue()) {
            if (!(tuple.getKeyNode() instanceof ScalarNode)) {
                return false;
            }
            if (!(tuple.getValueNode() instanceof ScalarNode)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isCompatibleEnvVars(Node envVarsNode) {
        if (!(envVarsNode instanceof MappingNode envVarsMapping)) {
            return false;
        }
        if (envVarsMapping.getValue().size() != 1) {
            return false;
        }
        return isKey(envVarsMapping.getValue().get(0).getKeyNode(), "env");
    }

    private static Node getMappingValue(MappingNode mapping, String key) {
        for (NodeTuple tuple : mapping.getValue()) {
            if (isKey(tuple.getKeyNode(), key)) {
                return tuple.getValueNode();
            }
        }
        return null;
    }

    private static boolean isKey(Node node, String expected) {
        return node instanceof ScalarNode && expected.equalsIgnoreCase(((ScalarNode) node).getValue());
    }

    private static String getScalarValue(MappingNode mapping, String key) {
        Node value = getMappingValue(mapping, key);
        if (value instanceof ScalarNode) {
            return ((ScalarNode) value).getValue();
        }
        return null;
    }

    @Override
    @CheckForNull
    public CNode describe(EnvironmentVariablesNodeProperty instance, ConfigurationContext context) throws Exception {
        Mapping mapping = new Mapping();
        for (Attribute attribute : getAttributes()) {
            CNode value = attribute.describe(instance, context);
            if (value != null) {
                // Making sure empty variables are part of the export
                value.asSequence().forEach(entry -> {
                    if (entry.asMapping().get("key") != null
                            && entry.asMapping().get("value") != null) {
                        entry.asMapping().get("value").asScalar().setPrintableWhenEmpty(true);
                    }
                });
                mapping.put(attribute.getName(), value);
            }
        }
        return mapping;
    }
}
