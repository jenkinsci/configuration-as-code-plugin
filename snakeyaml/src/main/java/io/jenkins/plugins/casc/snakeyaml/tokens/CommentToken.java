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

/**
 * @deprecated it will be removed because it is not used
 */
public class CommentToken extends Token {
    public CommentToken(Mark startMark, Mark endMark) {
        super(startMark, endMark);
    }

    @Override
    public ID getTokenId() {
        return ID.Comment;
    }
}
