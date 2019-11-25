package io.jenkins.plugins.casc.yaml;

import io.jenkins.plugins.casc.ConfiguratorException;
import org.yaml.snakeyaml.nodes.Node;

public interface MergeStrategy {

    void merge(Node root, Node node, String source) throws ConfiguratorException;

    String getName();
}
