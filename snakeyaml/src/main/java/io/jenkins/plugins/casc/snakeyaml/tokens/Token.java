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
package io.jenkins.plugins.casc.snakeyaml.tokens;

import io.jenkins.plugins.casc.snakeyaml.error.Mark;
import io.jenkins.plugins.casc.snakeyaml.error.YAMLException;

public abstract class Token {
    public enum ID {
        Alias("<alias>"),
        Anchor("<anchor>"),
        BlockEnd("<block end>"),
        BlockEntry("-"),
        BlockMappingStart("<block mapping start>"),
        BlockSequenceStart("<block sequence start>"),
        Directive("<directive>"),
        DocumentEnd("<document end>"),
        DocumentStart("<document start>"),
        FlowEntry(","),
        FlowMappingEnd("}"),
        FlowMappingStart("{"),
        FlowSequenceEnd("]"),
        FlowSequenceStart("["),
        Key("?"),
        Scalar("<scalar>"),
        StreamEnd("<stream end>"),
        StreamStart("<stream start>"),
        Tag("<tag>"),
        Value(":"),
        Whitespace("<whitespace>"),
        Comment("#"),
        Error("<error>");

        private final String description;

        ID(String s) {
            description = s;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    private final Mark startMark;
    private final Mark endMark;

    public Token(Mark startMark, Mark endMark) {
        if (startMark == null || endMark == null) {
            throw new YAMLException("Token requires marks.");
        }
        this.startMark = startMark;
        this.endMark = endMark;
    }

    public Mark getStartMark() {
        return startMark;
    }

    public Mark getEndMark() {
        return endMark;
    }

    /**
     * For error reporting.
     * 
     * @see "class variable 'id' in PyYAML"
     * @return ID of this token
     */
    public abstract Token.ID getTokenId();
}
