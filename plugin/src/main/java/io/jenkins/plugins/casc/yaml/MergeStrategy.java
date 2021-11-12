package io.jenkins.plugins.casc.yaml;

import io.jenkins.plugins.casc.ConfiguratorException;
import org.yaml.snakeyaml.nodes.Node;

/**
 * YAML merge strategy between multiple files
 */
public interface MergeStrategy {
    String DEFAULT_STRATEGY = "errorOnConflict";

    /**
     * Merge two nodes which come from two YAML files
     * @param firstNode the first node of a node list
     * @param secondNode the second node of a node list
     * @param source is the source of node
     * @throws ConfiguratorException if the merge fails
     */
    void merge(Node firstNode, Node secondNode, String source) throws ConfiguratorException;

    /**
     * Name of the merge strategy which must be unique.
     * @return name of the merge strategy
     */
    String getName();
}
