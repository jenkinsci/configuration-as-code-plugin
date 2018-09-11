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

import java.util.regex.Pattern;

import io.jenkins.plugins.casc.snakeyaml.nodes.Tag;

final class ResolverTuple {
    private final Tag tag;
    private final Pattern regexp;

    public ResolverTuple(Tag tag, Pattern regexp) {
        this.tag = tag;
        this.regexp = regexp;
    }

    public Tag getTag() {
        return tag;
    }

    public Pattern getRegexp() {
        return regexp;
    }

    @Override
    public String toString() {
        return "Tuple tag=" + tag + " regexp=" + regexp;
    }
}