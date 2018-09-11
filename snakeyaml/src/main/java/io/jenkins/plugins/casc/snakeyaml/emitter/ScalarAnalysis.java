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

public final class ScalarAnalysis {
    public String scalar;
    public boolean empty;
    public boolean multiline;
    public boolean allowFlowPlain;
    public boolean allowBlockPlain;
    public boolean allowSingleQuoted;
    public boolean allowBlock;

    public ScalarAnalysis(String scalar, boolean empty, boolean multiline, boolean allowFlowPlain,
            boolean allowBlockPlain, boolean allowSingleQuoted, boolean allowBlock) {
        this.scalar = scalar;
        this.empty = empty;
        this.multiline = multiline;
        this.allowFlowPlain = allowFlowPlain;
        this.allowBlockPlain = allowBlockPlain;
        this.allowSingleQuoted = allowSingleQuoted;
        this.allowBlock = allowBlock;
    }
}