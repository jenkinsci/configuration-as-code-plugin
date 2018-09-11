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
 * Represents a sequence.
 * <p>
 * A sequence is a ordered collection of nodes.
 * </p>
 */
public class SequenceNode extends CollectionNode<Node> {
    final private List<Node> value;

    public SequenceNode(Tag tag, boolean resolved, List<Node> value, Mark startMark, Mark endMark,
                        DumperOptions.FlowStyle flowStyle) {
        super(tag, startMark, endMark, flowStyle);
        if (value == null) {
            throw new NullPointerException("value in a Node is required.");
        }
        this.value = value;
        this.resolved = resolved;
    }

    public SequenceNode(Tag tag, List<Node> value, DumperOptions.FlowStyle flowStyle) {
        this(tag, true, value, null, null, flowStyle);
    }
    
    /*
     * Existed in older versions but replaced with {@link DumperOptions.SequenceStyle}-based constructor.
     * Restored in v1.22 for backwards compatibility.
     * @deprecated Since restored in v1.22.  Use {@link SequenceNode#SequenceNode(Tag, List<Node>, io.jenkins.plugins.casc.snakeyaml.DumperOptions.FlowStyle) }.
     */
    @Deprecated
    public SequenceNode(Tag tag, List<Node> value, Boolean style) {
        this(tag, value, DumperOptions.FlowStyle.fromBoolean(style));
    }

    /*
     * Existed in older versions but replaced with {@link DumperOptions.SequenceStyle}-based constructor.
     * Restored in v1.22 for backwards compatibility.
     * @deprecated Since restored in v1.22.  Use {@link SequenceNode#SequenceNode(Tag, boolean, List<Node>, Mark, Mark, io.jenkins.plugins.casc.snakeyaml.DumperOptions.FlowStyle) }.
     */
    @Deprecated
    public SequenceNode(Tag tag, boolean resolved, List<Node> value, Mark startMark, Mark endMark,
              Boolean style) {
        this(tag, resolved, value, startMark, endMark, DumperOptions.FlowStyle.fromBoolean(style));
    }

    @Override
    public NodeId getNodeId() {
        return NodeId.sequence;
    }

    /**
     * Returns the elements in this sequence.
     * 
     * @return Nodes in the specified order.
     */
    public List<Node> getValue() {
        return value;
    }

    public void setListType(Class<? extends Object> listType) {
        for (Node node : value) {
            node.setType(listType);
        }
    }

    public String toString() {
        return "<" + this.getClass().getName() + " (tag=" + getTag() + ", value=" + getValue()
                + ")>";
    }
}
