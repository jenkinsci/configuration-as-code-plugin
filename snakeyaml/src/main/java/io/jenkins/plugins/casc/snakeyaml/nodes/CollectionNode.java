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
 * Base class for the two collection types {@link MappingNode mapping} and
 * {@link SequenceNode collection}.
 */
public abstract class CollectionNode<T> extends Node {
    private DumperOptions.FlowStyle flowStyle;

    public CollectionNode(Tag tag, Mark startMark, Mark endMark, DumperOptions.FlowStyle flowStyle) {
        super(tag, startMark, endMark);
        setFlowStyle(flowStyle);
    }

    /*
     * Existed in older versions but replaced with {@link DumperOptions.FlowStyle}-based constructor.
     * Restored in v1.22 for backwards compatibility.
     * @deprecated Since restored in v1.22.  Use {@link CollectionNode#CollectionNode(Tag, Mark, Mark, io.jenkins.plugins.casc.snakeyaml.DumperOptions.FlowStyle) }.
     */
    @Deprecated
    public CollectionNode(Tag tag, Mark startMark, Mark endMark, Boolean flowStyle) {
        this(tag, startMark, endMark, DumperOptions.FlowStyle.fromBoolean(flowStyle));
    }

    /**
     * Returns the elements in this sequence.
     *
     * @return Nodes in the specified order.
     */
    abstract public List<T> getValue();

    /**
     * Serialization style of this collection.
     *
     * @return <code>true</code> for flow style, <code>false</code> for block
     *         style.
     */
    public DumperOptions.FlowStyle getFlowStyle() {
        return flowStyle;
    }

    public void setFlowStyle(DumperOptions.FlowStyle flowStyle) {
        if (flowStyle == null) throw new NullPointerException("Flow style must be provided.");
        this.flowStyle = flowStyle;
    }

    public void setEndMark(Mark endMark) {
        this.endMark = endMark;
    }
}
