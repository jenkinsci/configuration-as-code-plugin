package io.jenkins.plugins.casc.model;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.stream.IntStream;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */

public final class Scalar implements CNode, CharSequence {

    private static final String SECRET_VALUE_STRING = "****";

    private String value;
    private Format format;
    private boolean raw;
    private Source source;
    private boolean sensitive;
    private boolean encrypted;

    public enum Format { STRING, MULTILINESTRING, BOOLEAN, NUMBER, FLOATING }

    public Scalar(String value, Source source) {
        this(value);
        this.source = source;
    }

    public Scalar(String value) {
        this.value = value;
        this.format = value.contains("\n") ? Format.MULTILINESTRING : Format.STRING;
        this.raw = false;
    }

    public Scalar(Enum instance) {
        this.value = instance.name();
        this.format = Format.STRING;
        this.raw = true;
    }

    public Scalar(Boolean instance) {
        this.value = String.valueOf(instance);
        this.format = Format.BOOLEAN;
        this.raw = true;
    }

    public Scalar(Number instance) {
        this.value = String.valueOf(instance);
        this.raw = true;
        if (instance instanceof Float || instance instanceof Double) {
            this.format = Format.FLOATING;
        } else {
            this.format = Format.NUMBER;
        }
    }

    @Override
    public Type getType() {
        return Type.SCALAR;
    }

    public Format getFormat() {
        return format;
    }

    public boolean isRaw() {
        return raw;
    }

    @Override
    public Scalar asScalar() {
        return this;
    }

    /**
     * Gets value of the scalar for export.
     * @return Value of the scalar if not {@link #isMasked()},
     *         {@link #SECRET_VALUE_STRING} otherwise.
     *         Encrypted sensitive data will be returned as is.
     *
     */
    public String getValue() {
        return isMasked() ? SECRET_VALUE_STRING : value;
    }

    /**
     * Check whether the scalar value should be masked in the output.
     * @return {@code true} if the value is masked
     * @since 1.25
     */
    public boolean isMasked() {
        return sensitive && !encrypted;
    }

    /**
     * Sets the sensitive flag.
     * It indicates that the scalar represents a sensitive argument (secret or other restricted data).
     * @param sensitive value to set
     * @return Object instance
     * @since 1.25
     */
    public Scalar sensitive(boolean sensitive) {
        this.sensitive = sensitive;
        return this;
    }

    /**
     * Indicates that the data is encrypted and hence safe to be exported.
     * @param encrypted Value to set
     * @return Object instance
     * @since 1.25
     */
    public Scalar encrypted(boolean encrypted) {
        this.encrypted = encrypted;
        return this;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isSensitiveData() {
        return sensitive;
    }

    @NonNull
    @Override
    public String toString() {
        return value;
    }

    @Override
    public IntStream chars() {
        return value.chars();
    }

    @Override
    public IntStream codePoints() {
        return value.codePoints();
    }

    @Override
    public int length() {
        return value.length();
    }

    @Override
    public char charAt(int index) {
        return value.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return value.subSequence(start, end);
    }

    public Source getSource() {
        return source;
    }

    @Override
    public CNode clone() {
        return new Scalar(this);
    }

    private Scalar(Scalar it) {
        this.value = it.value;
        this.format = it.format;
        this.raw = it.raw;
        this.source = it.source;
        this.sensitive = it.sensitive;
        this.encrypted = it.encrypted;
    }
}
