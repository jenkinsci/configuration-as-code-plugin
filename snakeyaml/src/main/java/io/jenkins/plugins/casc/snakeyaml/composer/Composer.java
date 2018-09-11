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
package io.jenkins.plugins.casc.snakeyaml.composer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.jenkins.plugins.casc.snakeyaml.events.AliasEvent;
import io.jenkins.plugins.casc.snakeyaml.events.Event;
import io.jenkins.plugins.casc.snakeyaml.events.MappingStartEvent;
import io.jenkins.plugins.casc.snakeyaml.events.NodeEvent;
import io.jenkins.plugins.casc.snakeyaml.events.ScalarEvent;
import io.jenkins.plugins.casc.snakeyaml.events.SequenceStartEvent;
import io.jenkins.plugins.casc.snakeyaml.nodes.MappingNode;
import io.jenkins.plugins.casc.snakeyaml.nodes.Node;
import io.jenkins.plugins.casc.snakeyaml.nodes.NodeId;
import io.jenkins.plugins.casc.snakeyaml.nodes.NodeTuple;
import io.jenkins.plugins.casc.snakeyaml.nodes.ScalarNode;
import io.jenkins.plugins.casc.snakeyaml.nodes.SequenceNode;
import io.jenkins.plugins.casc.snakeyaml.nodes.Tag;
import io.jenkins.plugins.casc.snakeyaml.parser.Parser;
import io.jenkins.plugins.casc.snakeyaml.resolver.Resolver;

/**
 * Creates a node graph from parser events.
 * <p>
 * Corresponds to the 'Compose' step as described in chapter 3.1 of the
 * <a href="http://yaml.org/spec/1.1/">YAML Specification</a>.
 * </p>
 */
public class Composer {
    protected final Parser parser;
    private final Resolver resolver;
    private final Map<String, Node> anchors;
    private final Set<Node> recursiveNodes;

    public Composer(Parser parser, Resolver resolver) {
        this.parser = parser;
        this.resolver = resolver;
        this.anchors = new HashMap<String, Node>();
        this.recursiveNodes = new HashSet<Node>();
    }

    /**
     * Checks if further documents are available.
     *
     * @return <code>true</code> if there is at least one more document.
     */
    public boolean checkNode() {
        // Drop the STREAM-START event.
        if (parser.checkEvent(Event.ID.StreamStart)) {
            parser.getEvent();
        }
        // If there are more documents available?
        return !parser.checkEvent(Event.ID.StreamEnd);
    }

    /**
     * Reads and composes the next document.
     *
     * @return The root node of the document or <code>null</code> if no more
     * documents are available.
     */
    public Node getNode() {
        // Drop the DOCUMENT-START event.
        parser.getEvent();
        // Compose the root node.
        Node node = composeNode(null);
        // Drop the DOCUMENT-END event.
        parser.getEvent();
        this.anchors.clear();
        recursiveNodes.clear();
        return node;
    }

    /**
     * Reads a document from a source that contains only one document.
     * <p>
     * If the stream contains more than one document an exception is thrown.
     * </p>
     *
     * @return The root node of the document or <code>null</code> if no document
     * is available.
     */
    public Node getSingleNode() {
        // Drop the STREAM-START event.
        parser.getEvent();
        // Compose a document if the stream is not empty.
        Node document = null;
        if (!parser.checkEvent(Event.ID.StreamEnd)) {
            document = getNode();
        }
        // Ensure that the stream contains no more documents.
        if (!parser.checkEvent(Event.ID.StreamEnd)) {
            Event event = parser.getEvent();
            throw new ComposerException("expected a single document in the stream",
                    document.getStartMark(), "but found another document", event.getStartMark());
        }
        // Drop the STREAM-END event.
        parser.getEvent();
        return document;
    }

    private Node composeNode(Node parent) {
        if (parent != null) recursiveNodes.add(parent);
        final Node node;
        if (parser.checkEvent(Event.ID.Alias)) {
            AliasEvent event = (AliasEvent) parser.getEvent();
            String anchor = event.getAnchor();
            if (!anchors.containsKey(anchor)) {
                throw new ComposerException(null, null, "found undefined alias " + anchor,
                        event.getStartMark());
            }
            node = anchors.get(anchor);
            if (recursiveNodes.remove(node)) {
                node.setTwoStepsConstruction(true);
            }
        } else {
            NodeEvent event = (NodeEvent) parser.peekEvent();
            String anchor = event.getAnchor();
            // the check for duplicate anchors has been removed (issue 174)
            if (parser.checkEvent(Event.ID.Scalar)) {
                node = composeScalarNode(anchor);
            } else if (parser.checkEvent(Event.ID.SequenceStart)) {
                node = composeSequenceNode(anchor);
            } else {
                node = composeMappingNode(anchor);
            }
        }
        recursiveNodes.remove(parent);
        return node;
    }

    protected Node composeScalarNode(String anchor) {
        ScalarEvent ev = (ScalarEvent) parser.getEvent();
        String tag = ev.getTag();
        boolean resolved = false;
        Tag nodeTag;
        if (tag == null || tag.equals("!")) {
            nodeTag = resolver.resolve(NodeId.scalar, ev.getValue(),
                    ev.getImplicit().canOmitTagInPlainScalar());
            resolved = true;
        } else {
            nodeTag = new Tag(tag);
        }
        Node node = new ScalarNode(nodeTag, resolved, ev.getValue(), ev.getStartMark(),
                ev.getEndMark(), ev.getScalarStyle());
        if (anchor != null) {
            anchors.put(anchor, node);
        }
        return node;
    }

    protected Node composeSequenceNode(String anchor) {
        SequenceStartEvent startEvent = (SequenceStartEvent) parser.getEvent();
        String tag = startEvent.getTag();
        Tag nodeTag;
        boolean resolved = false;
        if (tag == null || tag.equals("!")) {
            nodeTag = resolver.resolve(NodeId.sequence, null, startEvent.getImplicit());
            resolved = true;
        } else {
            nodeTag = new Tag(tag);
        }
        final ArrayList<Node> children = new ArrayList<Node>();
        SequenceNode node = new SequenceNode(nodeTag, resolved, children, startEvent.getStartMark(),
                null, startEvent.getFlowStyle());
        if (anchor != null) {
            anchors.put(anchor, node);
        }
        while (!parser.checkEvent(Event.ID.SequenceEnd)) {
            children.add(composeNode(node));
        }
        Event endEvent = parser.getEvent();
        node.setEndMark(endEvent.getEndMark());
        return node;
    }

    protected Node composeMappingNode(String anchor) {
        MappingStartEvent startEvent = (MappingStartEvent) parser.getEvent();
        String tag = startEvent.getTag();
        Tag nodeTag;
        boolean resolved = false;
        if (tag == null || tag.equals("!")) {
            nodeTag = resolver.resolve(NodeId.mapping, null, startEvent.getImplicit());
            resolved = true;
        } else {
            nodeTag = new Tag(tag);
        }

        final List<NodeTuple> children = new ArrayList<NodeTuple>();
        MappingNode node = new MappingNode(nodeTag, resolved, children, startEvent.getStartMark(),
                null, startEvent.getFlowStyle());
        if (anchor != null) {
            anchors.put(anchor, node);
        }
        while (!parser.checkEvent(Event.ID.MappingEnd)) {
            composeMappingChildren(children, node);
        }
        Event endEvent = parser.getEvent();
        node.setEndMark(endEvent.getEndMark());
        return node;
    }

    protected void composeMappingChildren(List<NodeTuple> children, MappingNode node) {
        Node itemKey = composeKeyNode(node);
        if (itemKey.getTag().equals(Tag.MERGE)) {
            node.setMerged(true);
        }
        Node itemValue = composeValueNode(node);
        children.add(new NodeTuple(itemKey, itemValue));
    }

    protected Node composeKeyNode(MappingNode node) {
        return composeNode(node);
    }

    protected Node composeValueNode(MappingNode node) {
        return composeNode(node);
    }
}
