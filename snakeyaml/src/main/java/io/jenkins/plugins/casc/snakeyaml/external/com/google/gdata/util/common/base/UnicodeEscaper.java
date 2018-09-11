/* Copyright (c) 2008 Google Inc.
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

package io.jenkins.plugins.casc.snakeyaml.external.com.google.gdata.util.common.base;

import java.io.IOException;

/**
 * An {@link Escaper} that converts literal text into a format safe for
 * inclusion in a particular context (such as an XML document). Typically (but
 * not always), the inverse process of "unescaping" the text is performed
 * automatically by the relevant parser.
 * 
 * <p>
 * For example, an XML escaper would convert the literal string
 * {@code "Foo<Bar>"} into {@code "Foo&lt;Bar&gt;"} to prevent {@code "<Bar>"}
 * from being confused with an XML tag. When the resulting XML document is
 * parsed, the parser API will return this text as the original literal string
 * {@code "Foo<Bar>"}.
 * 
 * <p>
 * <b>Note:</b> This class is similar to {@link CharEscaper} but with one very
 * important difference. A CharEscaper can only process Java <a
 * href="http://en.wikipedia.org/wiki/UTF-16">UTF16</a> characters in isolation
 * and may not cope when it encounters surrogate pairs. This class facilitates
 * the correct escaping of all Unicode characters.
 * 
 * <p>
 * As there are important reasons, including potential security issues, to
 * handle Unicode correctly if you are considering implementing a new escaper
 * you should favor using UnicodeEscaper wherever possible.
 * 
 * <p>
 * A {@code UnicodeEscaper} instance is required to be stateless, and safe when
 * used concurrently by multiple threads.
 * 
 * <p>
 * Several popular escapers are defined as constants in the class
 * {@link CharEscapers}. To create your own escapers extend this class and
 * implement the {@link #escape(int)} method.
 * 
 * 
 */
public abstract class UnicodeEscaper implements Escaper {
    /** The amount of padding (chars) to use when growing the escape buffer. */
    private static final int DEST_PAD = 32;

    /**
     * Returns the escaped form of the given Unicode code point, or {@code null}
     * if this code point does not need to be escaped. When called as part of an
     * escaping operation, the given code point is guaranteed to be in the range
     * {@code 0 <= cp <= Character#MAX_CODE_POINT}.
     * 
     * <p>
     * If an empty array is returned, this effectively strips the input
     * character from the resulting text.
     * 
     * <p>
     * If the character does not need to be escaped, this method should return
     * {@code null}, rather than an array containing the character
     * representation of the code point. This enables the escaping algorithm to
     * perform more efficiently.
     * 
     * <p>
     * If the implementation of this method cannot correctly handle a particular
     * code point then it should either throw an appropriate runtime exception
     * or return a suitable replacement character. It must never silently
     * discard invalid input as this may constitute a security risk.
     * 
     * @param cp
     *            the Unicode code point to escape if necessary
     * @return the replacement characters, or {@code null} if no escaping was
     *         needed
     */
    protected abstract char[] escape(int cp);

    /**
     * Scans a sub-sequence of characters from a given {@link CharSequence},
     * returning the index of the next character that requires escaping.
     * 
     * <p>
     * <b>Note:</b> When implementing an escaper, it is a good idea to override
     * this method for efficiency. The base class implementation determines
     * successive Unicode code points and invokes {@link #escape(int)} for each
     * of them. If the semantics of your escaper are such that code points in
     * the supplementary range are either all escaped or all unescaped, this
     * method can be implemented more efficiently using
     * {@link CharSequence#charAt(int)}.
     * 
     * <p>
     * Note however that if your escaper does not escape characters in the
     * supplementary range, you should either continue to validate the
     * correctness of any surrogate characters encountered or provide a clear
     * warning to users that your escaper does not validate its input.
     * 
     * <p>
     * See {@link PercentEscaper} for an example.
     * 
     * @param csq
     *            a sequence of characters
     * @param start
     *            the index of the first character to be scanned
     * @param end
     *            the index immediately after the last character to be scanned
     * @throws IllegalArgumentException
     *             if the scanned sub-sequence of {@code csq} contains invalid
     *             surrogate pairs
     */
    protected int nextEscapeIndex(CharSequence csq, int start, int end) {
        int index = start;
        while (index < end) {
            int cp = codePointAt(csq, index, end);
            if (cp < 0 || escape(cp) != null) {
                break;
            }
            index += Character.isSupplementaryCodePoint(cp) ? 2 : 1;
        }
        return index;
    }

    /**
     * Returns the escaped form of a given literal string.
     * 
     * <p>
     * If you are escaping input in arbitrary successive chunks, then it is not
     * generally safe to use this method. If an input string ends with an
     * unmatched high surrogate character, then this method will throw
     * {@link IllegalArgumentException}. You should either ensure your input is
     * valid <a href="http://en.wikipedia.org/wiki/UTF-16">UTF-16</a> before
     * calling this method or use an escaped {@link Appendable} (as returned by
     * {@link #escape(Appendable)}) which can cope with arbitrarily split input.
     * 
     * <p>
     * <b>Note:</b> When implementing an escaper it is a good idea to override
     * this method for efficiency by inlining the implementation of
     * {@link #nextEscapeIndex(CharSequence, int, int)} directly. Doing this for
     * {@link PercentEscaper} more than doubled the performance for unescaped
     * strings (as measured by {@link CharEscapersBenchmark}).
     * 
     * @param string
     *            the literal string to be escaped
     * @return the escaped form of {@code string}
     * @throws NullPointerException
     *             if {@code string} is null
     * @throws IllegalArgumentException
     *             if invalid surrogate characters are encountered
     */
    public String escape(String string) {
        int end = string.length();
        int index = nextEscapeIndex(string, 0, end);
        return index == end ? string : escapeSlow(string, index);
    }

    /**
     * Returns the escaped form of a given literal string, starting at the given
     * index. This method is called by the {@link #escape(String)} method when
     * it discovers that escaping is required. It is protected to allow
     * subclasses to override the fastpath escaping function to inline their
     * escaping test. See {@link CharEscaperBuilder} for an example usage.
     * 
     * <p>
     * This method is not reentrant and may only be invoked by the top level
     * {@link #escape(String)} method.
     * 
     * @param s
     *            the literal string to be escaped
     * @param index
     *            the index to start escaping from
     * @return the escaped form of {@code string}
     * @throws NullPointerException
     *             if {@code string} is null
     * @throws IllegalArgumentException
     *             if invalid surrogate characters are encountered
     */
    protected final String escapeSlow(String s, int index) {
        int end = s.length();

        // Get a destination buffer and setup some loop variables.
        char[] dest = DEST_TL.get();
        int destIndex = 0;
        int unescapedChunkStart = 0;

        while (index < end) {
            int cp = codePointAt(s, index, end);
            if (cp < 0) {
                throw new IllegalArgumentException("Trailing high surrogate at end of input");
            }
            char[] escaped = escape(cp);
            if (escaped != null) {
                int charsSkipped = index - unescapedChunkStart;

                // This is the size needed to add the replacement, not the full
                // size needed by the string. We only regrow when we absolutely
                // must.
                int sizeNeeded = destIndex + charsSkipped + escaped.length;
                if (dest.length < sizeNeeded) {
                    int destLength = sizeNeeded + (end - index) + DEST_PAD;
                    dest = growBuffer(dest, destIndex, destLength);
                }
                // If we have skipped any characters, we need to copy them now.
                if (charsSkipped > 0) {
                    s.getChars(unescapedChunkStart, index, dest, destIndex);
                    destIndex += charsSkipped;
                }
                if (escaped.length > 0) {
                    System.arraycopy(escaped, 0, dest, destIndex, escaped.length);
                    destIndex += escaped.length;
                }
            }
            unescapedChunkStart = index + (Character.isSupplementaryCodePoint(cp) ? 2 : 1);
            index = nextEscapeIndex(s, unescapedChunkStart, end);
        }

        // Process trailing unescaped characters - no need to account for
        // escaped
        // length or padding the allocation.
        int charsSkipped = end - unescapedChunkStart;
        if (charsSkipped > 0) {
            int endIndex = destIndex + charsSkipped;
            if (dest.length < endIndex) {
                dest = growBuffer(dest, destIndex, endIndex);
            }
            s.getChars(unescapedChunkStart, end, dest, destIndex);
            destIndex = endIndex;
        }
        return new String(dest, 0, destIndex);
    }

    /**
     * Returns an {@code Appendable} instance which automatically escapes all
     * text appended to it before passing the resulting text to an underlying
     * {@code Appendable}.
     * 
     * <p>
     * Unlike {@link #escape(String)} it is permitted to append arbitrarily
     * split input to this Appendable, including input that is split over a
     * surrogate pair. In this case the pending high surrogate character will
     * not be processed until the corresponding low surrogate is appended. This
     * means that a trailing high surrogate character at the end of the input
     * cannot be detected and will be silently ignored. This is unavoidable
     * since the Appendable interface has no {@code close()} method, and it is
     * impossible to determine when the last characters have been appended.
     * 
     * <p>
     * The methods of the returned object will propagate any exceptions thrown
     * by the underlying {@code Appendable}.
     * 
     * <p>
     * For well formed <a href="http://en.wikipedia.org/wiki/UTF-16">UTF-16</a>
     * the escaping behavior is identical to that of {@link #escape(String)} and
     * the following code is equivalent to (but much slower than)
     * {@code escaper.escape(string)}:
     * 
     * <pre>
     * {
     *     &#064;code
     *     StringBuilder sb = new StringBuilder();
     *     escaper.escape(sb).append(string);
     *     return sb.toString();
     * }
     * </pre>
     * 
     * @param out
     *            the underlying {@code Appendable} to append escaped output to
     * @return an {@code Appendable} which passes text to {@code out} after
     *         escaping it
     * @throws NullPointerException
     *             if {@code out} is null
     * @throws IllegalArgumentException
     *             if invalid surrogate characters are encountered
     * 
     */
    public Appendable escape(final Appendable out) {
        assert out != null;

        return new Appendable() {
            int pendingHighSurrogate = -1;
            char[] decodedChars = new char[2];

            public Appendable append(CharSequence csq) throws IOException {
                return append(csq, 0, csq.length());
            }

            public Appendable append(CharSequence csq, int start, int end) throws IOException {
                int index = start;
                if (index < end) {
                    // This is a little subtle: index must never reference the
                    // middle of a
                    // surrogate pair but unescapedChunkStart can. The first
                    // time we enter
                    // the loop below it is possible that index !=
                    // unescapedChunkStart.
                    int unescapedChunkStart = index;
                    if (pendingHighSurrogate != -1) {
                        // Our last append operation ended halfway through a
                        // surrogate pair
                        // so we have to do some extra work first.
                        char c = csq.charAt(index++);
                        if (!Character.isLowSurrogate(c)) {
                            throw new IllegalArgumentException(
                                    "Expected low surrogate character but got " + c);
                        }
                        char[] escaped = escape(Character.toCodePoint((char) pendingHighSurrogate,
                                c));
                        if (escaped != null) {
                            // Emit the escaped character and adjust
                            // unescapedChunkStart to
                            // skip the low surrogate we have consumed.
                            outputChars(escaped, escaped.length);
                            unescapedChunkStart += 1;
                        } else {
                            // Emit pending high surrogate (unescaped) but do
                            // not modify
                            // unescapedChunkStart as we must still emit the low
                            // surrogate.
                            out.append((char) pendingHighSurrogate);
                        }
                        pendingHighSurrogate = -1;
                    }
                    while (true) {
                        // Find and append the next subsequence of unescaped
                        // characters.
                        index = nextEscapeIndex(csq, index, end);
                        if (index > unescapedChunkStart) {
                            out.append(csq, unescapedChunkStart, index);
                        }
                        if (index == end) {
                            break;
                        }
                        // If we are not finished, calculate the next code
                        // point.
                        int cp = codePointAt(csq, index, end);
                        if (cp < 0) {
                            // Our sequence ended half way through a surrogate
                            // pair so just
                            // record the state and exit.
                            pendingHighSurrogate = -cp;
                            break;
                        }
                        // Escape the code point and output the characters.
                        char[] escaped = escape(cp);
                        if (escaped != null) {
                            outputChars(escaped, escaped.length);
                        } else {
                            // This shouldn't really happen if nextEscapeIndex
                            // is correct but
                            // we should cope with false positives.
                            int len = Character.toChars(cp, decodedChars, 0);
                            outputChars(decodedChars, len);
                        }
                        // Update our index past the escaped character and
                        // continue.
                        index += (Character.isSupplementaryCodePoint(cp) ? 2 : 1);
                        unescapedChunkStart = index;
                    }
                }
                return this;
            }

            public Appendable append(char c) throws IOException {
                if (pendingHighSurrogate != -1) {
                    // Our last append operation ended halfway through a
                    // surrogate pair
                    // so we have to do some extra work first.
                    if (!Character.isLowSurrogate(c)) {
                        throw new IllegalArgumentException(
                                "Expected low surrogate character but got '" + c + "' with value "
                                        + (int) c);
                    }
                    char[] escaped = escape(Character.toCodePoint((char) pendingHighSurrogate, c));
                    if (escaped != null) {
                        outputChars(escaped, escaped.length);
                    } else {
                        out.append((char) pendingHighSurrogate);
                        out.append(c);
                    }
                    pendingHighSurrogate = -1;
                } else if (Character.isHighSurrogate(c)) {
                    // This is the start of a (split) surrogate pair.
                    pendingHighSurrogate = c;
                } else {
                    if (Character.isLowSurrogate(c)) {
                        throw new IllegalArgumentException("Unexpected low surrogate character '"
                                + c + "' with value " + (int) c);
                    }
                    // This is a normal (non surrogate) char.
                    char[] escaped = escape(c);
                    if (escaped != null) {
                        outputChars(escaped, escaped.length);
                    } else {
                        out.append(c);
                    }
                }
                return this;
            }

            private void outputChars(char[] chars, int len) throws IOException {
                for (int n = 0; n < len; n++) {
                    out.append(chars[n]);
                }
            }
        };
    }

    /**
     * Returns the Unicode code point of the character at the given index.
     * 
     * <p>
     * Unlike {@link Character#codePointAt(CharSequence, int)} or
     * {@link String#codePointAt(int)} this method will never fail silently when
     * encountering an invalid surrogate pair.
     * 
     * <p>
     * The behaviour of this method is as follows:
     * <ol>
     * <li>If {@code index >= end}, {@link IndexOutOfBoundsException} is thrown.
     * <li><b>If the character at the specified index is not a surrogate, it is
     * returned.</b>
     * <li>If the first character was a high surrogate value, then an attempt is
     * made to read the next character.
     * <ol>
     * <li><b>If the end of the sequence was reached, the negated value of the
     * trailing high surrogate is returned.</b>
     * <li><b>If the next character was a valid low surrogate, the code point
     * value of the high/low surrogate pair is returned.</b>
     * <li>If the next character was not a low surrogate value, then
     * {@link IllegalArgumentException} is thrown.
     * </ol>
     * <li>If the first character was a low surrogate value,
     * {@link IllegalArgumentException} is thrown.
     * </ol>
     * 
     * @param seq
     *            the sequence of characters from which to decode the code point
     * @param index
     *            the index of the first character to decode
     * @param end
     *            the index beyond the last valid character to decode
     * @return the Unicode code point for the given index or the negated value
     *         of the trailing high surrogate character at the end of the
     *         sequence
     */
    protected static final int codePointAt(CharSequence seq, int index, int end) {
        if (index < end) {
            char c1 = seq.charAt(index++);
            if (c1 < Character.MIN_HIGH_SURROGATE || c1 > Character.MAX_LOW_SURROGATE) {
                // Fast path (first test is probably all we need to do)
                return c1;
            } else if (c1 <= Character.MAX_HIGH_SURROGATE) {
                // If the high surrogate was the last character, return its
                // inverse
                if (index == end) {
                    return -c1;
                }
                // Otherwise look for the low surrogate following it
                char c2 = seq.charAt(index);
                if (Character.isLowSurrogate(c2)) {
                    return Character.toCodePoint(c1, c2);
                }
                throw new IllegalArgumentException("Expected low surrogate but got char '" + c2
                        + "' with value " + (int) c2 + " at index " + index);
            } else {
                throw new IllegalArgumentException("Unexpected low surrogate character '" + c1
                        + "' with value " + (int) c1 + " at index " + (index - 1));
            }
        }
        throw new IndexOutOfBoundsException("Index exceeds specified range");
    }

    /**
     * Helper method to grow the character buffer as needed, this only happens
     * once in a while so it's ok if it's in a method call. If the index passed
     * in is 0 then no copying will be done.
     */
    private static final char[] growBuffer(char[] dest, int index, int size) {
        char[] copy = new char[size];
        if (index > 0) {
            System.arraycopy(dest, 0, copy, 0, index);
        }
        return copy;
    }

    /**
     * A thread-local destination buffer to keep us from creating new buffers.
     * The starting size is 1024 characters. If we grow past this we don't put
     * it back in the threadlocal, we just keep going and grow as needed.
     */
    private static final ThreadLocal<char[]> DEST_TL = new ThreadLocal<char[]>() {
        @Override
        protected char[] initialValue() {
            return new char[1024];
        }
    };
}
