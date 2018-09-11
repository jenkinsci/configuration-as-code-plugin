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
package io.jenkins.plugins.casc.snakeyaml.parser;

import io.jenkins.plugins.casc.snakeyaml.error.Mark;
import io.jenkins.plugins.casc.snakeyaml.error.MarkedYAMLException;

/**
 * Exception thrown by the {@link Parser} implementations in case of malformed
 * input.
 */
public class ParserException extends MarkedYAMLException {
    private static final long serialVersionUID = -2349253802798398038L;

    /**
     * Constructs an instance.
     * 
     * @param context
     *            Part of the input document in which vicinity the problem
     *            occurred.
     * @param contextMark
     *            Position of the <code>context</code> within the document.
     * @param problem
     *            Part of the input document that caused the problem.
     * @param problemMark
     *            Position of the <code>problem</code>. within the document.
     */
    public ParserException(String context, Mark contextMark, String problem, Mark problemMark) {
        super(context, contextMark, problem, problemMark, null, null);
    }
}
