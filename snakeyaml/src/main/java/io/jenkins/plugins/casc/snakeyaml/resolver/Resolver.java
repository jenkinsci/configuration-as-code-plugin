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
package io.jenkins.plugins.casc.snakeyaml.resolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import io.jenkins.plugins.casc.snakeyaml.nodes.NodeId;
import io.jenkins.plugins.casc.snakeyaml.nodes.Tag;

/**
 * Resolver tries to detect a type by content (when the tag is implicit)
 */
public class Resolver {
    public static final Pattern BOOL = Pattern
            .compile("^(?:yes|Yes|YES|no|No|NO|true|True|TRUE|false|False|FALSE|on|On|ON|off|Off|OFF)$");

    /**
     * The regular expression is taken from the 1.2 specification but '_'s are
     * added to keep backwards compatibility
     */
    public static final Pattern FLOAT = Pattern
            .compile("^([-+]?(\\.[0-9]+|[0-9_]+(\\.[0-9_]*)?)([eE][-+]?[0-9]+)?|[-+]?[0-9][0-9_]*(?::[0-5]?[0-9])+\\.[0-9_]*|[-+]?\\.(?:inf|Inf|INF)|\\.(?:nan|NaN|NAN))$");
    public static final Pattern INT = Pattern
            .compile("^(?:[-+]?0b[0-1_]+|[-+]?0[0-7_]+|[-+]?(?:0|[1-9][0-9_]*)|[-+]?0x[0-9a-fA-F_]+|[-+]?[1-9][0-9_]*(?::[0-5]?[0-9])+)$");
    public static final Pattern MERGE = Pattern.compile("^(?:<<)$");
    public static final Pattern NULL = Pattern.compile("^(?:~|null|Null|NULL| )$");
    public static final Pattern EMPTY = Pattern.compile("^$");
    public static final Pattern TIMESTAMP = Pattern
            .compile("^(?:[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]|[0-9][0-9][0-9][0-9]-[0-9][0-9]?-[0-9][0-9]?(?:[Tt]|[ \t]+)[0-9][0-9]?:[0-9][0-9]:[0-9][0-9](?:\\.[0-9]*)?(?:[ \t]*(?:Z|[-+][0-9][0-9]?(?::[0-9][0-9])?))?)$");
    public static final Pattern VALUE = Pattern.compile("^(?:=)$");
    public static final Pattern YAML = Pattern.compile("^(?:!|&|\\*)$");

    protected Map<Character, List<ResolverTuple>> yamlImplicitResolvers = new HashMap<Character, List<ResolverTuple>>();

    protected void addImplicitResolvers() {
        addImplicitResolver(Tag.BOOL, BOOL, "yYnNtTfFoO");
        /*
         * INT must be before FLOAT because the regular expression for FLOAT
         * matches INT (see issue 130)
         * http://code.google.com/p/snakeyaml/issues/detail?id=130
         */
        addImplicitResolver(Tag.INT, INT, "-+0123456789");
        addImplicitResolver(Tag.FLOAT, FLOAT, "-+0123456789.");
        addImplicitResolver(Tag.MERGE, MERGE, "<");
        addImplicitResolver(Tag.NULL, NULL, "~nN\0");
        addImplicitResolver(Tag.NULL, EMPTY, null);
        addImplicitResolver(Tag.TIMESTAMP, TIMESTAMP, "0123456789");
        // The following implicit resolver is only for documentation
        // purposes.
        // It cannot work
        // because plain scalars cannot start with '!', '&', or '*'.
        addImplicitResolver(Tag.YAML, YAML, "!&*");
    }

    public Resolver() {
        addImplicitResolvers();
    }

    public void addImplicitResolver(Tag tag, Pattern regexp, String first) {
        if (first == null) {
            List<ResolverTuple> curr = yamlImplicitResolvers.get(null);
            if (curr == null) {
                curr = new ArrayList<ResolverTuple>();
                yamlImplicitResolvers.put(null, curr);
            }
            curr.add(new ResolverTuple(tag, regexp));
        } else {
            char[] chrs = first.toCharArray();
            for (int i = 0, j = chrs.length; i < j; i++) {
                Character theC = Character.valueOf(chrs[i]);
                if (theC == 0) {
                    // special case: for null
                    theC = null;
                }
                List<ResolverTuple> curr = yamlImplicitResolvers.get(theC);
                if (curr == null) {
                    curr = new ArrayList<ResolverTuple>();
                    yamlImplicitResolvers.put(theC, curr);
                }
                curr.add(new ResolverTuple(tag, regexp));
            }
        }
    }

    public Tag resolve(NodeId kind, String value, boolean implicit) {
        if (kind == NodeId.scalar && implicit) {
            final List<ResolverTuple> resolvers;
            if (value.length() == 0) {
                resolvers = yamlImplicitResolvers.get('\0');
            } else {
                resolvers = yamlImplicitResolvers.get(value.charAt(0));
            }
            if (resolvers != null) {
                for (ResolverTuple v : resolvers) {
                    Tag tag = v.getTag();
                    Pattern regexp = v.getRegexp();
                    if (regexp.matcher(value).matches()) {
                        return tag;
                    }
                }
            }
            if (yamlImplicitResolvers.containsKey(null)) {
                for (ResolverTuple v : yamlImplicitResolvers.get(null)) {
                    Tag tag = v.getTag();
                    Pattern regexp = v.getRegexp();
                    if (regexp.matcher(value).matches()) {
                        return tag;
                    }
                }
            }
        }
        switch (kind) {
        case scalar:
            return Tag.STR;
        case sequence:
            return Tag.SEQ;
        default:
            return Tag.MAP;
        }
    }
}