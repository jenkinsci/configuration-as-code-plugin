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
package io.jenkins.plugins.casc.snakeyaml.reader;

import io.jenkins.plugins.casc.snakeyaml.error.YAMLException;

public class ReaderException extends YAMLException {
    private static final long serialVersionUID = 8710781187529689083L;
    private final String name;
    private final int codePoint;
    private final int position;

    public ReaderException(String name, int position, int codePoint, String message) {
        super(message);
        this.name = name;
        this.codePoint = codePoint;
        this.position = position;
    }

    public String getName() {
        return name;
    }

    public int getCodePoint() {
        return codePoint;
    }

    public int getPosition() {
        return position;
    }

    @Override
    public String toString() {
        final String s = new String(Character.toChars(codePoint));
        return "unacceptable code point '" + s + "' (0x"
                + Integer.toHexString(codePoint).toUpperCase() + ") " + getMessage()
                + "\nin \"" + name + "\", position " + position;
    }
}
