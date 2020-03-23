package io.jenkins.plugins.casc.yaml;

import hudson.Extension;
import io.jenkins.plugins.casc.snakeyaml.nodes.Node;

/**
 * Only use the last one of all YAML config files. We can switch different configurations
 * base on this strategy.
 */
@Extension
public class OrderMergeStrategy implements MergeStrategy {

    @Override
    public Node merge(Node firstNode, Node secondNode, String source) {
        return secondNode;
    }

    @Override
    public String getName() {
        return "order";
    }
}
