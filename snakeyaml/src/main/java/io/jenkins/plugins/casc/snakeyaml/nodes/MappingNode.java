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

import java.util.List;

import io.jenkins.plugins.casc.snakeyaml.DumperOptions;
import io.jenkins.plugins.casc.snakeyaml.error.Mark;

/**
 * Represents a map.
 * <p>
 * A map is a collection of unsorted key-value pairs.
 * </p>
 */
public class MappingNode extends CollectionNode<NodeTuple> {
    private List<NodeTuple> value;
    private boolean merged = false;

    public MappingNode(Tag tag, boolean resolved, List<NodeTuple> value, Mark startMark,
            Mark endMark, DumperOptions.FlowStyle flowStyle) {
        super(tag, startMark, endMark, flowStyle);
        if (value == null) {
            throw new NullPointerException("value in a Node is required.");
        }
        this.value = value;
        this.resolved = resolved;
    }

    public MappingNode(Tag tag, List<NodeTuple> value, DumperOptions.FlowStyle flowStyle) {
        this(tag, true, value, null, null, flowStyle);
    }

    /*
     * Existed in older versions but replaced with {@link DumperOptions.FlowStyle}-based constructor.
     * Restored in v1.22 for backwards compatibility.
     * @deprecated Since restored in v1.22.  Use {@link MappingNode#MappingNode(Tag, boolean, List, Mark, Mark, io.jenkins.plugins.casc.snakeyaml.DumperOptions.FlowStyle) }.
     */
    @Deprecated
    public MappingNode(Tag tag, boolean resolved, List<NodeTuple> value, Mark startMark,
            Mark endMark, Boolean flowStyle) {
        this(tag, resolved, value, startMark, endMark, DumperOptions.FlowStyle.fromBoolean(flowStyle));
    }
    
    /*
     * Existed in older versions but replaced with {@link DumperOptions.FlowStyle}-based constructor.
     * Restored in v1.22 for backwards compatibility.
     * @deprecated Since restored in v1.22.  Use {@link MappingNode#MappingNode(Tag, List, io.jenkins.plugins.casc.snakeyaml.DumperOptions.FlowStyle) }.
     */
    @Deprecated
    public MappingNode(Tag tag, List<NodeTuple> value, Boolean flowStyle) {
        this(tag, value, DumperOptions.FlowStyle.fromBoolean(flowStyle));
        
    }
    
    @Override
    public NodeId getNodeId() {
        return NodeId.mapping;
    }

    /**
     * Returns the entries of this map.
     * 
     * @return List of entries.
     */
    public List<NodeTuple> getValue() {
        return value;
    }

    public void setValue(List<NodeTuple> mergedValue) {
        value = mergedValue;
    }

    public void setOnlyKeyType(Class<? extends Object> keyType) {
        for (NodeTuple nodes : value) {
            nodes.getKeyNode().setType(keyType);
        }
    }

    public void setTypes(Class<? extends Object> keyType, Class<? extends Object> valueType) {
        for (NodeTuple nodes : value) {
            nodes.getValueNode().setType(valueType);
            nodes.getKeyNode().setType(keyType);
        }
    }

    @Override
    public String toString() {
        String values;
        StringBuilder buf = new StringBuilder();
        for (NodeTuple node : getValue()) {
            buf.append("{ key=");
            buf.append(node.getKeyNode());
            buf.append("; value=");
            if (node.getValueNode() instanceof CollectionNode) {
                // to avoid overflow in case of recursive structures
                buf.append(System.identityHashCode(node.getValueNode()));
            } else {
                buf.append(node.toString());
            }
            buf.append(" }");
        }
        values = buf.toString();
        return "<" + this.getClass().getName() + " (tag=" + getTag() + ", values=" + values + ")>";
    }

    /**
     * @param merged
     *            - true if map contains merge node
     */
    public void setMerged(boolean merged) {
        this.merged = merged;
    }

    /**
     * @return true if map contains merge node
     */
    public boolean isMerged() {
        return merged;
    }
}
