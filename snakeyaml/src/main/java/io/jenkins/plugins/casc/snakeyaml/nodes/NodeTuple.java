/**
 * Copyright (c) 2008, http://www.snakeyaml.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.jenkins.plugins.casc.snakeyaml.nodes;

/**
 * Stores one key value pair used in a map.
 */
public final class NodeTuple {

    private Node keyNode;
    private Node valueNode;

    public NodeTuple(Node keyNode, Node valueNode) {
        if (keyNode == null || valueNode == null) {
            throw new NullPointerException("Nodes must be provided.");
        }
        this.keyNode = keyNode;
        this.valueNode = valueNode;
    }

    /**
     * Key node.
     *
     * @return the node used as key
     */
    public Node getKeyNode() {
        return keyNode;
    }

    /**
     * Value node.
     * 
     * @return node used as value
     */
    public Node getValueNode() {
        return valueNode;
    }

    @Override
    public String toString() {
        return "<NodeTuple keyNode=" + keyNode.toString() + "; valueNode=" + valueNode.toString()
                + ">";
    }
}
