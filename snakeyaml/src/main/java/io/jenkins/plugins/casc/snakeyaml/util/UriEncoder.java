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
package io.jenkins.plugins.casc.snakeyaml.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;

import io.jenkins.plugins.casc.snakeyaml.error.YAMLException;
import io.jenkins.plugins.casc.snakeyaml.external.com.google.gdata.util.common.base.Escaper;
import io.jenkins.plugins.casc.snakeyaml.external.com.google.gdata.util.common.base.PercentEscaper;

public abstract class UriEncoder {
    private static final CharsetDecoder UTF8Decoder = Charset.forName("UTF-8").newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT);
    // Include the [] chars to the SAFEPATHCHARS_URLENCODER to avoid
    // its escape as required by spec. See
    // http://yaml.org/spec/1.1/#escaping%20in%20URI/
    private static final String SAFE_CHARS = PercentEscaper.SAFEPATHCHARS_URLENCODER + "[]/";
    private static final Escaper escaper = new PercentEscaper(SAFE_CHARS, false);

    /**
     * Escape special characters with '%'
     * @param uri URI to be escaped
     * @return encoded URI
     */
    public static String encode(String uri) {
        return escaper.escape(uri);
    }

    /**
     * Decode '%'-escaped characters. Decoding fails in case of invalid UTF-8
     * @param buff data to decode
     * @return decoded data
     * @throws CharacterCodingException if cannot be decoded
     */
    public static String decode(ByteBuffer buff) throws CharacterCodingException {
        CharBuffer chars = UTF8Decoder.decode(buff);
        return chars.toString();
    }

    public static String decode(String buff) {
        try {
            return URLDecoder.decode(buff, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new YAMLException(e);
        }
    }
}
