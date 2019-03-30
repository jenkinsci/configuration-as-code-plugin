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

/**
 version: 1.1 / 2007-01-25
 - changed BOM recognition ordering (longer boms first)

 Original pseudocode   : Thomas Weidenfeller
 Implementation tweaked: Aki Nieminen
 Implementation changed: Andrey Somov 
 * UTF-32 removed because it is not supported by YAML
 * no default encoding

 http://www.unicode.org/unicode/faq/utf_bom.html
 BOMs:
 00 00 FE FF    = UTF-32, big-endian
 FF FE 00 00    = UTF-32, little-endian
 EF BB BF       = UTF-8,
 FE FF          = UTF-16, big-endian
 FF FE          = UTF-16, little-endian

 Win2k Notepad:
 Unicode format = UTF-16LE
 ***/

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;

/**
 * Generic unicode textreader, which will use BOM mark to identify the encoding
 * to be used. If BOM is not found then use a given default or system encoding.
 */
public class UnicodeReader extends Reader {
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final Charset UTF16BE = Charset.forName("UTF-16BE");
    private static final Charset UTF16LE = Charset.forName("UTF-16LE");

    PushbackInputStream internalIn;
    InputStreamReader internalIn2 = null;

    private static final int BOM_SIZE = 3;

    /**
     * @param in
     *            InputStream to be read
     */
    public UnicodeReader(InputStream in) {
        internalIn = new PushbackInputStream(in, BOM_SIZE);
    }

    /**
     * Get stream encoding or NULL if stream is uninitialized. Call init() or
     * read() method to initialize it.
     * @return the name of the character encoding being used by this stream.
     */
    public String getEncoding() {
        return internalIn2.getEncoding();
    }

    /**
     * Read-ahead four bytes and check for BOM marks. Extra bytes are unread
     * back to the stream, only BOM bytes are skipped.
     * @throws  IOException if InputStream cannot be created
     */
    protected void init() throws IOException {
        if (internalIn2 != null)
            return;

        Charset encoding;
        byte bom[] = new byte[BOM_SIZE];
        int n, unread;
        n = internalIn.read(bom, 0, bom.length);

        if ((bom[0] == (byte) 0xEF) && (bom[1] == (byte) 0xBB) && (bom[2] == (byte) 0xBF)) {
            encoding = UTF8;
            unread = n - 3;
        } else if ((bom[0] == (byte) 0xFE) && (bom[1] == (byte) 0xFF)) {
            encoding = UTF16BE;
            unread = n - 2;
        } else if ((bom[0] == (byte) 0xFF) && (bom[1] == (byte) 0xFE)) {
            encoding = UTF16LE;
            unread = n - 2;
        } else {
            // Unicode BOM mark not found, unread all bytes
            encoding = UTF8;
            unread = n;
        }

        if (unread > 0)
            internalIn.unread(bom, (n - unread), unread);

        // Use given encoding
        CharsetDecoder decoder = encoding.newDecoder().onUnmappableCharacter(
                CodingErrorAction.REPORT);
        internalIn2 = new InputStreamReader(internalIn, decoder);
    }

    public void close() throws IOException {
        init();
        internalIn2.close();
    }

    public int read(@NonNull char[] cbuf, int off, int len) throws IOException {
        init();
        return internalIn2.read(cbuf, off, len);
    }
}
