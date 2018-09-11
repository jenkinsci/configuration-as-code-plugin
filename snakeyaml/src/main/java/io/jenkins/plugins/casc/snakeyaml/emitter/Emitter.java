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
package io.jenkins.plugins.casc.snakeyaml.emitter;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.regex.Pattern;

import io.jenkins.plugins.casc.snakeyaml.DumperOptions;
import io.jenkins.plugins.casc.snakeyaml.DumperOptions.Version;
import io.jenkins.plugins.casc.snakeyaml.error.YAMLException;
import io.jenkins.plugins.casc.snakeyaml.events.AliasEvent;
import io.jenkins.plugins.casc.snakeyaml.events.CollectionEndEvent;
import io.jenkins.plugins.casc.snakeyaml.events.CollectionStartEvent;
import io.jenkins.plugins.casc.snakeyaml.events.DocumentEndEvent;
import io.jenkins.plugins.casc.snakeyaml.events.DocumentStartEvent;
import io.jenkins.plugins.casc.snakeyaml.events.Event;
import io.jenkins.plugins.casc.snakeyaml.events.MappingEndEvent;
import io.jenkins.plugins.casc.snakeyaml.events.MappingStartEvent;
import io.jenkins.plugins.casc.snakeyaml.events.NodeEvent;
import io.jenkins.plugins.casc.snakeyaml.events.ScalarEvent;
import io.jenkins.plugins.casc.snakeyaml.events.SequenceEndEvent;
import io.jenkins.plugins.casc.snakeyaml.events.SequenceStartEvent;
import io.jenkins.plugins.casc.snakeyaml.events.StreamEndEvent;
import io.jenkins.plugins.casc.snakeyaml.events.StreamStartEvent;
import io.jenkins.plugins.casc.snakeyaml.nodes.Tag;
import io.jenkins.plugins.casc.snakeyaml.reader.StreamReader;
import io.jenkins.plugins.casc.snakeyaml.scanner.Constant;
import io.jenkins.plugins.casc.snakeyaml.util.ArrayStack;

/**
 * <pre>
 * Emitter expects events obeying the following grammar:
 * stream ::= STREAM-START document* STREAM-END
 * document ::= DOCUMENT-START node DOCUMENT-END
 * node ::= SCALAR | sequence | mapping
 * sequence ::= SEQUENCE-START node* SEQUENCE-END
 * mapping ::= MAPPING-START (node node)* MAPPING-END
 * </pre>
 */
public final class Emitter implements Emitable {
    private static final Map<Character, String> ESCAPE_REPLACEMENTS = new HashMap<Character, String>();
    public static final int MIN_INDENT = 1;
    public static final int MAX_INDENT = 10;

    private static final char[] SPACE = { ' ' };

    static {
        ESCAPE_REPLACEMENTS.put('\0', "0");
        ESCAPE_REPLACEMENTS.put('\u0007', "a");
        ESCAPE_REPLACEMENTS.put('\u0008', "b");
        ESCAPE_REPLACEMENTS.put('\u0009', "t");
        ESCAPE_REPLACEMENTS.put('\n', "n");
        ESCAPE_REPLACEMENTS.put('\u000B', "v");
        ESCAPE_REPLACEMENTS.put('\u000C', "f");
        ESCAPE_REPLACEMENTS.put('\r', "r");
        ESCAPE_REPLACEMENTS.put('\u001B', "e");
        ESCAPE_REPLACEMENTS.put('"', "\"");
        ESCAPE_REPLACEMENTS.put('\\', "\\");
        ESCAPE_REPLACEMENTS.put('\u0085', "N");
        ESCAPE_REPLACEMENTS.put('\u00A0', "_");
        ESCAPE_REPLACEMENTS.put('\u2028', "L");
        ESCAPE_REPLACEMENTS.put('\u2029', "P");
    }

    private final static Map<String, String> DEFAULT_TAG_PREFIXES = new LinkedHashMap<String, String>();
    static {
        DEFAULT_TAG_PREFIXES.put("!", "!");
        DEFAULT_TAG_PREFIXES.put(Tag.PREFIX, "!!");
    }
    // The stream should have the methods `write` and possibly `flush`.
    private final Writer stream;

    // Encoding is defined by Writer (cannot be overriden by STREAM-START.)
    // private Charset encoding;

    // Emitter is a state machine with a stack of states to handle nested
    // structures.
    private final ArrayStack<EmitterState> states;
    private EmitterState state;

    // Current event and the event queue.
    private final Queue<Event> events;
    private Event event;

    // The current indentation level and the stack of previous indents.
    private final ArrayStack<Integer> indents;
    private Integer indent;

    // Flow level.
    private int flowLevel;

    // Contexts.
    private boolean rootContext;
    private boolean mappingContext;
    private boolean simpleKeyContext;

    //
    // Characteristics of the last emitted character:
    // - current position.
    // - is it a whitespace?
    // - is it an indention character
    // (indentation space, '-', '?', or ':')?
    // private int line; this variable is not used
    private int column;
    private boolean whitespace;
    private boolean indention;
    private boolean openEnded;

    // Formatting details.
    private Boolean canonical;
    // pretty print flow by adding extra line breaks
    private Boolean prettyFlow;

    private boolean allowUnicode;
    private int bestIndent;
    private int indicatorIndent;
    private int bestWidth;
    private char[] bestLineBreak;
    private boolean splitLines;

    // Tag prefixes.
    private Map<String, String> tagPrefixes;

    // Prepared anchor and tag.
    private String preparedAnchor;
    private String preparedTag;

    // Scalar analysis and style.
    private ScalarAnalysis analysis;
    private DumperOptions.ScalarStyle style;

    public Emitter(Writer stream, DumperOptions opts) {
        // The stream should have the methods `write` and possibly `flush`.
        this.stream = stream;
        // Emitter is a state machine with a stack of states to handle nested
        // structures.
        this.states = new ArrayStack<EmitterState>(100);
        this.state = new ExpectStreamStart();
        // Current event and the event queue.
        this.events = new ArrayBlockingQueue<Event>(100);
        this.event = null;
        // The current indentation level and the stack of previous indents.
        this.indents = new ArrayStack<Integer>(10);
        this.indent = null;
        // Flow level.
        this.flowLevel = 0;
        // Contexts.
        mappingContext = false;
        simpleKeyContext = false;

        //
        // Characteristics of the last emitted character:
        // - current position.
        // - is it a whitespace?
        // - is it an indention character
        // (indentation space, '-', '?', or ':')?
        column = 0;
        whitespace = true;
        indention = true;

        // Whether the document requires an explicit document indicator
        openEnded = false;

        // Formatting details.
        this.canonical = opts.isCanonical();
        this.prettyFlow = opts.isPrettyFlow();
        this.allowUnicode = opts.isAllowUnicode();
        this.bestIndent = 2;
        if ((opts.getIndent() > MIN_INDENT) && (opts.getIndent() < MAX_INDENT)) {
            this.bestIndent = opts.getIndent();
        }
        this.indicatorIndent = opts.getIndicatorIndent();
        this.bestWidth = 80;
        if (opts.getWidth() > this.bestIndent * 2) {
            this.bestWidth = opts.getWidth();
        }
        this.bestLineBreak = opts.getLineBreak().getString().toCharArray();
        this.splitLines = opts.getSplitLines();

        // Tag prefixes.
        this.tagPrefixes = new LinkedHashMap<String, String>();

        // Prepared anchor and tag.
        this.preparedAnchor = null;
        this.preparedTag = null;

        // Scalar analysis and style.
        this.analysis = null;
        this.style = null;
    }

    public void emit(Event event) throws IOException {
        this.events.add(event);
        while (!needMoreEvents()) {
            this.event = this.events.poll();
            this.state.expect();
            this.event = null;
        }
    }

    // In some cases, we wait for a few next events before emitting.

    private boolean needMoreEvents() {
        if (events.isEmpty()) {
            return true;
        }
        Event event = events.peek();
        if (event instanceof DocumentStartEvent) {
            return needEvents(1);
        } else if (event instanceof SequenceStartEvent) {
            return needEvents(2);
        } else if (event instanceof MappingStartEvent) {
            return needEvents(3);
        } else {
            return false;
        }
    }

    private boolean needEvents(int count) {
        int level = 0;
        Iterator<Event> iter = events.iterator();
        iter.next();
        while (iter.hasNext()) {
            Event event = iter.next();
            if (event instanceof DocumentStartEvent || event instanceof CollectionStartEvent) {
                level++;
            } else if (event instanceof DocumentEndEvent || event instanceof CollectionEndEvent) {
                level--;
            } else if (event instanceof StreamEndEvent) {
                level = -1;
            }
            if (level < 0) {
                return false;
            }
        }
        return events.size() < count + 1;
    }

    private void increaseIndent(boolean flow, boolean indentless) {
        indents.push(indent);
        if (indent == null) {
            if (flow) {
                indent = bestIndent;
            } else {
                indent = 0;
            }
        } else if (!indentless) {
            this.indent += bestIndent;
        }
    }

    // States

    // Stream handlers.

    private class ExpectStreamStart implements EmitterState {
        public void expect() throws IOException {
            if (event instanceof StreamStartEvent) {
                writeStreamStart();
                state = new ExpectFirstDocumentStart();
            } else {
                throw new EmitterException("expected StreamStartEvent, but got " + event);
            }
        }
    }

    private class ExpectNothing implements EmitterState {
        public void expect() throws IOException {
            throw new EmitterException("expecting nothing, but got " + event);
        }
    }

    // Document handlers.

    private class ExpectFirstDocumentStart implements EmitterState {
        public void expect() throws IOException {
            new ExpectDocumentStart(true).expect();
        }
    }

    private class ExpectDocumentStart implements EmitterState {
        private boolean first;

        public ExpectDocumentStart(boolean first) {
            this.first = first;
        }

        public void expect() throws IOException {
            if (event instanceof DocumentStartEvent) {
                DocumentStartEvent ev = (DocumentStartEvent) event;
                if ((ev.getVersion() != null || ev.getTags() != null) && openEnded) {
                    writeIndicator("...", true, false, false);
                    writeIndent();
                }
                if (ev.getVersion() != null) {
                    String versionText = prepareVersion(ev.getVersion());
                    writeVersionDirective(versionText);
                }
                tagPrefixes = new LinkedHashMap<String, String>(DEFAULT_TAG_PREFIXES);
                if (ev.getTags() != null) {
                    Set<String> handles = new TreeSet<String>(ev.getTags().keySet());
                    for (String handle : handles) {
                        String prefix = ev.getTags().get(handle);
                        tagPrefixes.put(prefix, handle);
                        String handleText = prepareTagHandle(handle);
                        String prefixText = prepareTagPrefix(prefix);
                        writeTagDirective(handleText, prefixText);
                    }
                }
                boolean implicit = first && !ev.getExplicit() && !canonical
                        && ev.getVersion() == null
                        && (ev.getTags() == null || ev.getTags().isEmpty())
                        && !checkEmptyDocument();
                if (!implicit) {
                    writeIndent();
                    writeIndicator("---", true, false, false);
                    if (canonical) {
                        writeIndent();
                    }
                }
                state = new ExpectDocumentRoot();
            } else if (event instanceof StreamEndEvent) {
                // TODO fix 313 PyYAML changeset
                // if (openEnded) {
                // writeIndicator("...", true, false, false);
                // writeIndent();
                // }
                writeStreamEnd();
                state = new ExpectNothing();
            } else {
                throw new EmitterException("expected DocumentStartEvent, but got " + event);
            }
        }
    }

    private class ExpectDocumentEnd implements EmitterState {
        public void expect() throws IOException {
            if (event instanceof DocumentEndEvent) {
                writeIndent();
                if (((DocumentEndEvent) event).getExplicit()) {
                    writeIndicator("...", true, false, false);
                    writeIndent();
                }
                flushStream();
                state = new ExpectDocumentStart(false);
            } else {
                throw new EmitterException("expected DocumentEndEvent, but got " + event);
            }
        }
    }

    private class ExpectDocumentRoot implements EmitterState {
        public void expect() throws IOException {
            states.push(new ExpectDocumentEnd());
            expectNode(true, false, false);
        }
    }

    // Node handlers.

    private void expectNode(boolean root, boolean mapping, boolean simpleKey) throws IOException {
        rootContext = root;
        mappingContext = mapping;
        simpleKeyContext = simpleKey;
        if (event instanceof AliasEvent) {
            expectAlias();
        } else if (event instanceof ScalarEvent || event instanceof CollectionStartEvent) {
            processAnchor("&");
            processTag();
            if (event instanceof ScalarEvent) {
                expectScalar();
            } else if (event instanceof SequenceStartEvent) {
                if (flowLevel != 0 || canonical || ((SequenceStartEvent) event).isFlow()
                        || checkEmptySequence()) {
                    expectFlowSequence();
                } else {
                    expectBlockSequence();
                }
            } else {// MappingStartEvent
                if (flowLevel != 0 || canonical || ((MappingStartEvent) event).isFlow()
                        || checkEmptyMapping()) {
                    expectFlowMapping();
                } else {
                    expectBlockMapping();
                }
            }
        } else {
            throw new EmitterException("expected NodeEvent, but got " + event);
        }
    }

    private void expectAlias() throws IOException {
        if (((NodeEvent) event).getAnchor() == null) {
            throw new EmitterException("anchor is not specified for alias");
        }
        processAnchor("*");
        state = states.pop();
    }

    private void expectScalar() throws IOException {
        increaseIndent(true, false);
        processScalar();
        indent = indents.pop();
        state = states.pop();
    }

    // Flow sequence handlers.

    private void expectFlowSequence() throws IOException {
        writeIndicator("[", true, true, false);
        flowLevel++;
        increaseIndent(true, false);
        if (prettyFlow) {
            writeIndent();
        }
        state = new ExpectFirstFlowSequenceItem();
    }

    private class ExpectFirstFlowSequenceItem implements EmitterState {
        public void expect() throws IOException {
            if (event instanceof SequenceEndEvent) {
                indent = indents.pop();
                flowLevel--;
                writeIndicator("]", false, false, false);
                state = states.pop();
            } else {
                if (canonical || (column > bestWidth && splitLines) || prettyFlow) {
                    writeIndent();
                }
                states.push(new ExpectFlowSequenceItem());
                expectNode(false, false, false);
            }
        }
    }

    private class ExpectFlowSequenceItem implements EmitterState {
        public void expect() throws IOException {
            if (event instanceof SequenceEndEvent) {
                indent = indents.pop();
                flowLevel--;
                if (canonical) {
                    writeIndicator(",", false, false, false);
                    writeIndent();
                }
                writeIndicator("]", false, false, false);
                if (prettyFlow) {
                    writeIndent();
                }
                state = states.pop();
            } else {
                writeIndicator(",", false, false, false);
                if (canonical || (column > bestWidth && splitLines) || prettyFlow) {
                    writeIndent();
                }
                states.push(new ExpectFlowSequenceItem());
                expectNode(false, false, false);
            }
        }
    }

    // Flow mapping handlers.

    private void expectFlowMapping() throws IOException {
        writeIndicator("{", true, true, false);
        flowLevel++;
        increaseIndent(true, false);
        if (prettyFlow) {
            writeIndent();
        }
        state = new ExpectFirstFlowMappingKey();
    }

    private class ExpectFirstFlowMappingKey implements EmitterState {
        public void expect() throws IOException {
            if (event instanceof MappingEndEvent) {
                indent = indents.pop();
                flowLevel--;
                writeIndicator("}", false, false, false);
                state = states.pop();
            } else {
                if (canonical || (column > bestWidth && splitLines) || prettyFlow) {
                    writeIndent();
                }
                if (!canonical && checkSimpleKey()) {
                    states.push(new ExpectFlowMappingSimpleValue());
                    expectNode(false, true, true);
                } else {
                    writeIndicator("?", true, false, false);
                    states.push(new ExpectFlowMappingValue());
                    expectNode(false, true, false);
                }
            }
        }
    }

    private class ExpectFlowMappingKey implements EmitterState {
        public void expect() throws IOException {
            if (event instanceof MappingEndEvent) {
                indent = indents.pop();
                flowLevel--;
                if (canonical) {
                    writeIndicator(",", false, false, false);
                    writeIndent();
                }
                if (prettyFlow) {
                    writeIndent();
                }
                writeIndicator("}", false, false, false);
                state = states.pop();
            } else {
                writeIndicator(",", false, false, false);
                if (canonical || (column > bestWidth && splitLines) || prettyFlow) {
                    writeIndent();
                }
                if (!canonical && checkSimpleKey()) {
                    states.push(new ExpectFlowMappingSimpleValue());
                    expectNode(false, true, true);
                } else {
                    writeIndicator("?", true, false, false);
                    states.push(new ExpectFlowMappingValue());
                    expectNode(false, true, false);
                }
            }
        }
    }

    private class ExpectFlowMappingSimpleValue implements EmitterState {
        public void expect() throws IOException {
            writeIndicator(":", false, false, false);
            states.push(new ExpectFlowMappingKey());
            expectNode(false, true, false);
        }
    }

    private class ExpectFlowMappingValue implements EmitterState {
        public void expect() throws IOException {
            if (canonical || (column > bestWidth) || prettyFlow) {
                writeIndent();
            }
            writeIndicator(":", true, false, false);
            states.push(new ExpectFlowMappingKey());
            expectNode(false, true, false);
        }
    }

    // Block sequence handlers.

    private void expectBlockSequence() throws IOException {
        boolean indentless = mappingContext && !indention;
        increaseIndent(false, indentless);
        state = new ExpectFirstBlockSequenceItem();
    }

    private class ExpectFirstBlockSequenceItem implements EmitterState {
        public void expect() throws IOException {
            new ExpectBlockSequenceItem(true).expect();
        }
    }

    private class ExpectBlockSequenceItem implements EmitterState {
        private boolean first;

        public ExpectBlockSequenceItem(boolean first) {
            this.first = first;
        }

        public void expect() throws IOException {
            if (!this.first && event instanceof SequenceEndEvent) {
                indent = indents.pop();
                state = states.pop();
            } else {
                writeIndent();
                writeWhitespace(indicatorIndent);
                writeIndicator("-", true, false, true);
                states.push(new ExpectBlockSequenceItem(false));
                expectNode(false, false, false);
            }
        }
    }

    // Block mapping handlers.
    private void expectBlockMapping() throws IOException {
        increaseIndent(false, false);
        state = new ExpectFirstBlockMappingKey();
    }

    private class ExpectFirstBlockMappingKey implements EmitterState {
        public void expect() throws IOException {
            new ExpectBlockMappingKey(true).expect();
        }
    }

    private class ExpectBlockMappingKey implements EmitterState {
        private boolean first;

        public ExpectBlockMappingKey(boolean first) {
            this.first = first;
        }

        public void expect() throws IOException {
            if (!this.first && event instanceof MappingEndEvent) {
                indent = indents.pop();
                state = states.pop();
            } else {
                writeIndent();
                if (checkSimpleKey()) {
                    states.push(new ExpectBlockMappingSimpleValue());
                    expectNode(false, true, true);
                } else {
                    writeIndicator("?", true, false, true);
                    states.push(new ExpectBlockMappingValue());
                    expectNode(false, true, false);
                }
            }
        }
    }

    private class ExpectBlockMappingSimpleValue implements EmitterState {
        public void expect() throws IOException {
            writeIndicator(":", false, false, false);
            states.push(new ExpectBlockMappingKey(false));
            expectNode(false, true, false);
        }
    }

    private class ExpectBlockMappingValue implements EmitterState {
        public void expect() throws IOException {
            writeIndent();
            writeIndicator(":", true, false, true);
            states.push(new ExpectBlockMappingKey(false));
            expectNode(false, true, false);
        }
    }

    // Checkers.

    private boolean checkEmptySequence() {
        return event instanceof SequenceStartEvent && !events.isEmpty() && events.peek() instanceof SequenceEndEvent;
    }

    private boolean checkEmptyMapping() {
        return event instanceof MappingStartEvent && !events.isEmpty() && events.peek() instanceof MappingEndEvent;
    }

    private boolean checkEmptyDocument() {
        if (!(event instanceof DocumentStartEvent) || events.isEmpty()) {
            return false;
        }
        Event event = events.peek();
        if (event instanceof ScalarEvent) {
            ScalarEvent e = (ScalarEvent) event;
            return e.getAnchor() == null && e.getTag() == null && e.getImplicit() != null && e
                    .getValue().length() == 0;
        }
        return false;
    }

    private boolean checkSimpleKey() {
        int length = 0;
        if (event instanceof NodeEvent && ((NodeEvent) event).getAnchor() != null) {
            if (preparedAnchor == null) {
                preparedAnchor = prepareAnchor(((NodeEvent) event).getAnchor());
            }
            length += preparedAnchor.length();
        }
        String tag = null;
        if (event instanceof ScalarEvent) {
            tag = ((ScalarEvent) event).getTag();
        } else if (event instanceof CollectionStartEvent) {
            tag = ((CollectionStartEvent) event).getTag();
        }
        if (tag != null) {
            if (preparedTag == null) {
                preparedTag = prepareTag(tag);
            }
            length += preparedTag.length();
        }
        if (event instanceof ScalarEvent) {
            if (analysis == null) {
                analysis = analyzeScalar(((ScalarEvent) event).getValue());
            }
            length += analysis.scalar.length();
        }
        return length < 128 && (event instanceof AliasEvent
                || (event instanceof ScalarEvent && !analysis.empty && !analysis.multiline)
                || checkEmptySequence() || checkEmptyMapping());
    }

    // Anchor, Tag, and Scalar processors.

    private void processAnchor(String indicator) throws IOException {
        NodeEvent ev = (NodeEvent) event;
        if (ev.getAnchor() == null) {
            preparedAnchor = null;
            return;
        }
        if (preparedAnchor == null) {
            preparedAnchor = prepareAnchor(ev.getAnchor());
        }
        writeIndicator(indicator + preparedAnchor, true, false, false);
        preparedAnchor = null;
    }

    private void processTag() throws IOException {
        String tag = null;
        if (event instanceof ScalarEvent) {
            ScalarEvent ev = (ScalarEvent) event;
            tag = ev.getTag();
            if (style == null) {
                style = chooseScalarStyle();
            }
            if ((!canonical || tag == null) && ((style == null && ev.getImplicit()
                    .canOmitTagInPlainScalar()) || (style != null && ev.getImplicit()
                    .canOmitTagInNonPlainScalar()))) {
                preparedTag = null;
                return;
            }
            if (ev.getImplicit().canOmitTagInPlainScalar() && tag == null) {
                tag = "!";
                preparedTag = null;
            }
        } else {
            CollectionStartEvent ev = (CollectionStartEvent) event;
            tag = ev.getTag();
            if ((!canonical || tag == null) && ev.getImplicit()) {
                preparedTag = null;
                return;
            }
        }
        if (tag == null) {
            throw new EmitterException("tag is not specified");
        }
        if (preparedTag == null) {
            preparedTag = prepareTag(tag);
        }
        writeIndicator(preparedTag, true, false, false);
        preparedTag = null;
    }

    private DumperOptions.ScalarStyle chooseScalarStyle() {
        ScalarEvent ev = (ScalarEvent) event;
        if (analysis == null) {
            analysis = analyzeScalar(ev.getValue());
        }
        if (!ev.isPlain() && ev.getScalarStyle() == DumperOptions.ScalarStyle.DOUBLE_QUOTED || this.canonical) {
            return DumperOptions.ScalarStyle.DOUBLE_QUOTED;
        }
        if (ev.isPlain() && ev.getImplicit().canOmitTagInPlainScalar()) {
            if (!(simpleKeyContext && (analysis.empty || analysis.multiline))
                    && ((flowLevel != 0 && analysis.allowFlowPlain) || (flowLevel == 0 && analysis.allowBlockPlain))) {
                return null;
            }
        }
        if (!ev.isPlain() && (ev.getScalarStyle() == DumperOptions.ScalarStyle.LITERAL || ev.getScalarStyle() == DumperOptions.ScalarStyle.FOLDED)) {
            if (flowLevel == 0 && !simpleKeyContext && analysis.allowBlock) {
                return ev.getScalarStyle();
            }
        }
        if (ev.isPlain() || ev.getScalarStyle() == DumperOptions.ScalarStyle.SINGLE_QUOTED) {
            if (analysis.allowSingleQuoted && !(simpleKeyContext && analysis.multiline)) {
                return DumperOptions.ScalarStyle.SINGLE_QUOTED;
            }
        }
        return DumperOptions.ScalarStyle.DOUBLE_QUOTED;
    }

    private void processScalar() throws IOException {
        ScalarEvent ev = (ScalarEvent) event;
        if (analysis == null) {
            analysis = analyzeScalar(ev.getValue());
        }
        if (style == null) {
            style = chooseScalarStyle();
        }
        boolean split = !simpleKeyContext && splitLines;
        if (style == null) {
            writePlain(analysis.scalar, split);
        } else {
            switch (style) {
            case DOUBLE_QUOTED:
                writeDoubleQuoted(analysis.scalar, split);
                break;
                case SINGLE_QUOTED:
                writeSingleQuoted(analysis.scalar, split);
                break;
                case FOLDED:
                writeFolded(analysis.scalar, split);
                break;
                case LITERAL:
                writeLiteral(analysis.scalar);
                break;
            default:
                throw new YAMLException("Unexpected style: " + style);
            }
        }
        analysis = null;
        style = null;
    }

    // Analyzers.

    private String prepareVersion(Version version) {
        if (version.major() != 1) {
            throw new EmitterException("unsupported YAML version: " + version);
        }
        return version.getRepresentation();
    }

    private final static Pattern HANDLE_FORMAT = Pattern.compile("^![-_\\w]*!$");

    private String prepareTagHandle(String handle) {
        if (handle.length() == 0) {
            throw new EmitterException("tag handle must not be empty");
        } else if (handle.charAt(0) != '!' || handle.charAt(handle.length() - 1) != '!') {
            throw new EmitterException("tag handle must start and end with '!': " + handle);
        } else if (!"!".equals(handle) && !HANDLE_FORMAT.matcher(handle).matches()) {
            throw new EmitterException("invalid character in the tag handle: " + handle);
        }
        return handle;
    }

    private String prepareTagPrefix(String prefix) {
        if (prefix.length() == 0) {
            throw new EmitterException("tag prefix must not be empty");
        }
        StringBuilder chunks = new StringBuilder();
        int start = 0;
        int end = 0;
        if (prefix.charAt(0) == '!') {
            end = 1;
        }
        while (end < prefix.length()) {
            end++;
        }
        if (start < end) {
            chunks.append(prefix.substring(start, end));
        }
        return chunks.toString();
    }

    private String prepareTag(String tag) {
        if (tag.length() == 0) {
            throw new EmitterException("tag must not be empty");
        }
        if ("!".equals(tag)) {
            return tag;
        }
        String handle = null;
        String suffix = tag;
        // shall the tag prefixes be sorted as in PyYAML?
        for (String prefix : tagPrefixes.keySet()) {
            if (tag.startsWith(prefix) && ("!".equals(prefix) || prefix.length() < tag.length())) {
                handle = prefix;
            }
        }
        if (handle != null) {
            suffix = tag.substring(handle.length());
            handle = tagPrefixes.get(handle);
        }

        int end = suffix.length();
        String suffixText = end > 0 ? suffix.substring(0, end) : "";

        if (handle != null) {
            return handle + suffixText;
        }
        return "!<" + suffixText + ">";
    }

    private final static Pattern ANCHOR_FORMAT = Pattern.compile("^[-_\\w]*$");

    static String prepareAnchor(String anchor) {
        if (anchor.length() == 0) {
            throw new EmitterException("anchor must not be empty");
        }
        if (!ANCHOR_FORMAT.matcher(anchor).matches()) {
            throw new EmitterException("invalid character in the anchor: " + anchor);
        }
        return anchor;
    }

    private ScalarAnalysis analyzeScalar(String scalar) {
        // Empty scalar is a special case.
        if (scalar.length() == 0) {
            return new ScalarAnalysis(scalar, true, false, false, true, true, false);
        }
        // Indicators and special characters.
        boolean blockIndicators = false;
        boolean flowIndicators = false;
        boolean lineBreaks = false;
        boolean specialCharacters = false;

        // Important whitespace combinations.
        boolean leadingSpace = false;
        boolean leadingBreak = false;
        boolean trailingSpace = false;
        boolean trailingBreak = false;
        boolean breakSpace = false;
        boolean spaceBreak = false;

        // Check document indicators.
        if (scalar.startsWith("---") || scalar.startsWith("...")) {
            blockIndicators = true;
            flowIndicators = true;
        }
        // First character or preceded by a whitespace.
        boolean preceededByWhitespace = true;
        boolean followedByWhitespace = scalar.length() == 1 || Constant.NULL_BL_T_LINEBR.has(scalar.codePointAt(1));
        // The previous character is a space.
        boolean previousSpace = false;

        // The previous character is a break.
        boolean previousBreak = false;

        int index = 0;

        while (index < scalar.length()) {
            int c = scalar.codePointAt(index);
            // Check for indicators.
            if (index == 0) {
                // Leading indicators are special characters.
                if ("#,[]{}&*!|>\'\"%@`".indexOf(c) != -1) {
                    flowIndicators = true;
                    blockIndicators = true;
                }
                if (c == '?' || c == ':') {
                    flowIndicators = true;
                    if (followedByWhitespace) {
                        blockIndicators = true;
                    }
                }
                if (c == '-' && followedByWhitespace) {
                    flowIndicators = true;
                    blockIndicators = true;
                }
            } else {
                // Some indicators cannot appear within a scalar as well.
                if (",?[]{}".indexOf(c) != -1) {
                    flowIndicators = true;
                }
                if (c == ':') {
                    flowIndicators = true;
                    if (followedByWhitespace) {
                        blockIndicators = true;
                    }
                }
                if (c == '#' && preceededByWhitespace) {
                    flowIndicators = true;
                    blockIndicators = true;
                }
            }
            // Check for line breaks, special, and unicode characters.
            boolean isLineBreak = Constant.LINEBR.has(c);
            if (isLineBreak) {
                lineBreaks = true;
            }
            if (!(c == '\n' || (0x20 <= c && c <= 0x7E))) {
                if (c == 0x85 || (c >= 0xA0 && c <= 0xD7FF)
                || (c >= 0xE000 && c <= 0xFFFD)
                || (c >= 0x10000 && c <= 0x10FFFF)) {
                    // unicode is used
                    if (!this.allowUnicode) {
                        specialCharacters = true;
                    }
                } else {
                    specialCharacters = true;
                }
            }
            // Detect important whitespace combinations.
            if (c == ' ') {
                if (index == 0) {
                    leadingSpace = true;
                }
                if (index == scalar.length() - 1) {
                    trailingSpace = true;
                }
                if (previousBreak) {
                    breakSpace = true;
                }
                previousSpace = true;
                previousBreak = false;
            } else if (isLineBreak) {
                if (index == 0) {
                    leadingBreak = true;
                }
                if (index == scalar.length() - 1) {
                    trailingBreak = true;
                }
                if (previousSpace) {
                    spaceBreak = true;
                }
                previousSpace = false;
                previousBreak = true;
            } else {
                previousSpace = false;
                previousBreak = false;
            }

            // Prepare for the next character.
            index += Character.charCount(c);
            preceededByWhitespace = Constant.NULL_BL_T.has(c) || isLineBreak;
            followedByWhitespace = true;
            if (index + 1 < scalar.length()) {
                int nextIndex = index + Character.charCount(scalar.codePointAt(index));
                if (nextIndex < scalar.length()) {
                    followedByWhitespace = (Constant.NULL_BL_T.has(scalar.codePointAt(nextIndex))) || isLineBreak;
                }
            }
        }
        // Let's decide what styles are allowed.
        boolean allowFlowPlain = true;
        boolean allowBlockPlain = true;
        boolean allowSingleQuoted = true;
        boolean allowBlock = true;
        // Leading and trailing whitespaces are bad for plain scalars.
        if (leadingSpace || leadingBreak || trailingSpace || trailingBreak) {
            allowFlowPlain = allowBlockPlain = false;
        }
        // We do not permit trailing spaces for block scalars.
        if (trailingSpace) {
            allowBlock = false;
        }
        // Spaces at the beginning of a new line are only acceptable for block
        // scalars.
        if (breakSpace) {
            allowFlowPlain = allowBlockPlain = allowSingleQuoted = false;
        }
        // Spaces followed by breaks, as well as special character are only
        // allowed for double quoted scalars.
        if (spaceBreak || specialCharacters) {
            allowFlowPlain = allowBlockPlain = allowSingleQuoted = allowBlock = false;
        }
        // Although the plain scalar writer supports breaks, we never emit
        // multiline plain scalars in the flow context.
        if (lineBreaks) {
            allowFlowPlain = false;
        }
        // Flow indicators are forbidden for flow plain scalars.
        if (flowIndicators) {
            allowFlowPlain = false;
        }
        // Block indicators are forbidden for block plain scalars.
        if (blockIndicators) {
            allowBlockPlain = false;
        }

        return new ScalarAnalysis(scalar, false, lineBreaks, allowFlowPlain, allowBlockPlain,
                allowSingleQuoted, allowBlock);
    }

    // Writers.

    void flushStream() throws IOException {
        stream.flush();
    }

    void writeStreamStart() {
        // BOM is written by Writer.
    }

    void writeStreamEnd() throws IOException {
        flushStream();
    }

    void writeIndicator(String indicator, boolean needWhitespace, boolean whitespace,
            boolean indentation) throws IOException {
        if (!this.whitespace && needWhitespace) {
            this.column++;
            stream.write(SPACE);
        }
        this.whitespace = whitespace;
        this.indention = this.indention && indentation;
        this.column += indicator.length();
        openEnded = false;
        stream.write(indicator);
    }

    void writeIndent() throws IOException {
        int indent;
        if (this.indent != null) {
            indent = this.indent;
        } else {
            indent = 0;
        }

        if (!this.indention || this.column > indent || (this.column == indent && !this.whitespace)) {
            writeLineBreak(null);
        }

        writeWhitespace(indent - this.column);
    }

    private void writeWhitespace(int length) throws IOException {
        if (length <= 0) {
            return;
        }
        this.whitespace = true;
        char[] data = new char[length];
        for (int i = 0; i < data.length; i++) {
            data[i] = ' ';
        }
        this.column += length;
        stream.write(data);
    }

    private void writeLineBreak(String data) throws IOException {
        this.whitespace = true;
        this.indention = true;
        this.column = 0;
        if (data == null) {
            stream.write(this.bestLineBreak);
        } else {
            stream.write(data);
        }
    }

    void writeVersionDirective(String versionText) throws IOException {
        stream.write("%YAML ");
        stream.write(versionText);
        writeLineBreak(null);
    }

    void writeTagDirective(String handleText, String prefixText) throws IOException {
        // XXX: not sure 4 invocations better then StringBuilders created by str
        // + str
        stream.write("%TAG ");
        stream.write(handleText);
        stream.write(SPACE);
        stream.write(prefixText);
        writeLineBreak(null);
    }

    // Scalar streams.
    private void writeSingleQuoted(String text, boolean split) throws IOException {
        writeIndicator("'", true, false, false);
        boolean spaces = false;
        boolean breaks = false;
        int start = 0, end = 0;
        char ch;
        while (end <= text.length()) {
            ch = 0;
            if (end < text.length()) {
                ch = text.charAt(end);
            }
            if (spaces) {
                if (ch == 0 || ch != ' ') {
                    if (start + 1 == end && this.column > this.bestWidth && split && start != 0
                            && end != text.length()) {
                        writeIndent();
                    } else {
                        int len = end - start;
                        this.column += len;
                        stream.write(text, start, len);
                    }
                    start = end;
                }
            } else if (breaks) {
                if (ch == 0 || Constant.LINEBR.hasNo(ch)) {
                    if (text.charAt(start) == '\n') {
                        writeLineBreak(null);
                    }
                    String data = text.substring(start, end);
                    for (char br : data.toCharArray()) {
                        if (br == '\n') {
                            writeLineBreak(null);
                        } else {
                            writeLineBreak(String.valueOf(br));
                        }
                    }
                    writeIndent();
                    start = end;
                }
            } else {
                if (Constant.LINEBR.has(ch, "\0 \'")) {
                    if (start < end) {
                        int len = end - start;
                        this.column += len;
                        stream.write(text, start, len);
                        start = end;
                    }
                }
            }
            if (ch == '\'') {
                this.column += 2;
                stream.write("''");
                start = end + 1;
            }
            if (ch != 0) {
                spaces = ch == ' ';
                breaks = Constant.LINEBR.has(ch);
            }
            end++;
        }
        writeIndicator("'", false, false, false);
    }

    private void writeDoubleQuoted(String text, boolean split) throws IOException {
        writeIndicator("\"", true, false, false);
        int start = 0;
        int end = 0;
        while (end <= text.length()) {
            Character ch = null;
            if (end < text.length()) {
                ch = text.charAt(end);
            }
            if (ch == null || "\"\\\u0085\u2028\u2029\uFEFF".indexOf(ch) != -1
                    || !('\u0020' <= ch && ch <= '\u007E')) {
                if (start < end) {
                    int len = end - start;
                    this.column += len;
                    stream.write(text, start, len);
                    start = end;
                }
                if (ch != null) {
                    String data;
                    if (ESCAPE_REPLACEMENTS.containsKey(ch)) {
                        data = "\\" + ESCAPE_REPLACEMENTS.get(ch);
                    } else if (!this.allowUnicode || !StreamReader.isPrintable(ch)) {
                        // if !allowUnicode or the character is not printable,
                        // we must encode it
                        if (ch <= '\u00FF') {
                            String s = "0" + Integer.toString(ch, 16);
                            data = "\\x" + s.substring(s.length() - 2);
                        } else if (ch >= '\uD800' && ch <= '\uDBFF') {
                            if (end + 1 < text.length()) {
                                Character ch2 = text.charAt(++end);
                                String s = "000" + Long.toHexString(Character.toCodePoint(ch, ch2));
                                data = "\\U" + s.substring(s.length() - 8);
                            } else {
                                String s = "000" + Integer.toString(ch, 16);
                                data = "\\u" + s.substring(s.length() - 4);
                            }
                        } else {
                            String s = "000" + Integer.toString(ch, 16);
                            data = "\\u" + s.substring(s.length() - 4);
                        }
                    } else {
                        data = String.valueOf(ch);
                    }
                    this.column += data.length();
                    stream.write(data);
                    start = end + 1;
                }
            }
            if ((0 < end && end < (text.length() - 1)) && (ch == ' ' || start >= end)
                    && (this.column + (end - start)) > this.bestWidth && split) {
                String data;
                if (start >= end) {
                    data = "\\";
                } else {
                    data = text.substring(start, end) + "\\";
                }
                if (start < end) {
                    start = end;
                }
                this.column += data.length();
                stream.write(data);
                writeIndent();
                this.whitespace = false;
                this.indention = false;
                if (text.charAt(start) == ' ') {
                    data = "\\";
                    this.column += data.length();
                    stream.write(data);
                }
            }
            end += 1;
        }
        writeIndicator("\"", false, false, false);
    }

    private String determineBlockHints(String text) {
        StringBuilder hints = new StringBuilder();
        if (Constant.LINEBR.has(text.charAt(0), " ")) {
            hints.append(bestIndent);
        }
        char ch1 = text.charAt(text.length() - 1);
        if (Constant.LINEBR.hasNo(ch1)) {
            hints.append("-");
        } else if (text.length() == 1 || Constant.LINEBR.has(text.charAt(text.length() - 2))) {
            hints.append("+");
        }
        return hints.toString();
    }

    void writeFolded(String text, boolean split) throws IOException {
        String hints = determineBlockHints(text);
        writeIndicator(">" + hints, true, false, false);
        if (hints.length() > 0 && (hints.charAt(hints.length() - 1) == '+')) {
            openEnded = true;
        }
        writeLineBreak(null);
        boolean leadingSpace = true;
        boolean spaces = false;
        boolean breaks = true;
        int start = 0, end = 0;
        while (end <= text.length()) {
            char ch = 0;
            if (end < text.length()) {
                ch = text.charAt(end);
            }
            if (breaks) {
                if (ch == 0 || Constant.LINEBR.hasNo(ch)) {
                    if (!leadingSpace && ch != 0 && ch != ' ' && text.charAt(start) == '\n') {
                        writeLineBreak(null);
                    }
                    leadingSpace = ch == ' ';
                    String data = text.substring(start, end);
                    for (char br : data.toCharArray()) {
                        if (br == '\n') {
                            writeLineBreak(null);
                        } else {
                            writeLineBreak(String.valueOf(br));
                        }
                    }
                    if (ch != 0) {
                        writeIndent();
                    }
                    start = end;
                }
            } else if (spaces) {
                if (ch != ' ') {
                    if (start + 1 == end && this.column > this.bestWidth && split) {
                        writeIndent();
                    } else {
                        int len = end - start;
                        this.column += len;
                        stream.write(text, start, len);
                    }
                    start = end;
                }
            } else {
                if (Constant.LINEBR.has(ch, "\0 ")) {
                    int len = end - start;
                    this.column += len;
                    stream.write(text, start, len);
                    if (ch == 0) {
                        writeLineBreak(null);
                    }
                    start = end;
                }
            }
            if (ch != 0) {
                breaks = Constant.LINEBR.has(ch);
                spaces = ch == ' ';
            }
            end++;
        }
    }

    void writeLiteral(String text) throws IOException {
        String hints = determineBlockHints(text);
        writeIndicator("|" + hints, true, false, false);
        if (hints.length() > 0 && (hints.charAt(hints.length() - 1)) == '+') {
            openEnded = true;
        }
        writeLineBreak(null);
        boolean breaks = true;
        int start = 0, end = 0;
        while (end <= text.length()) {
            char ch = 0;
            if (end < text.length()) {
                ch = text.charAt(end);
            }
            if (breaks) {
                if (ch == 0 || Constant.LINEBR.hasNo(ch)) {
                    String data = text.substring(start, end);
                    for (char br : data.toCharArray()) {
                        if (br == '\n') {
                            writeLineBreak(null);
                        } else {
                            writeLineBreak(String.valueOf(br));
                        }
                    }
                    if (ch != 0) {
                        writeIndent();
                    }
                    start = end;
                }
            } else {
                if (ch == 0 || Constant.LINEBR.has(ch)) {
                    stream.write(text, start, end - start);
                    if (ch == 0) {
                        writeLineBreak(null);
                    }
                    start = end;
                }
            }
            if (ch != 0) {
                breaks = Constant.LINEBR.has(ch);
            }
            end++;
        }
    }

    void writePlain(String text, boolean split) throws IOException {
        if (rootContext) {
            openEnded = true;
        }
        if (text.length() == 0) {
            return;
        }
        if (!this.whitespace) {
            this.column++;
            stream.write(SPACE);
        }
        this.whitespace = false;
        this.indention = false;
        boolean spaces = false;
        boolean breaks = false;
        int start = 0, end = 0;
        while (end <= text.length()) {
            char ch = 0;
            if (end < text.length()) {
                ch = text.charAt(end);
            }
            if (spaces) {
                if (ch != ' ') {
                    if (start + 1 == end && this.column > this.bestWidth && split) {
                        writeIndent();
                        this.whitespace = false;
                        this.indention = false;
                    } else {
                        int len = end - start;
                        this.column += len;
                        stream.write(text, start, len);
                    }
                    start = end;
                }
            } else if (breaks) {
                if (Constant.LINEBR.hasNo(ch)) {
                    if (text.charAt(start) == '\n') {
                        writeLineBreak(null);
                    }
                    String data = text.substring(start, end);
                    for (char br : data.toCharArray()) {
                        if (br == '\n') {
                            writeLineBreak(null);
                        } else {
                            writeLineBreak(String.valueOf(br));
                        }
                    }
                    writeIndent();
                    this.whitespace = false;
                    this.indention = false;
                    start = end;
                }
            } else {
                if (Constant.LINEBR.has(ch, "\0 ")) {
                    int len = end - start;
                    this.column += len;
                    stream.write(text, start, len);
                    start = end;
                }
            }
            if (ch != 0) {
                spaces = ch == ' ';
                breaks = Constant.LINEBR.has(ch);
            }
            end++;
        }
    }
}
