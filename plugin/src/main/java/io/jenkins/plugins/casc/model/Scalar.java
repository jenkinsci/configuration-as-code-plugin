package io.jenkins.plugins.casc.model;

import io.jenkins.plugins.casc.SecretSource;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.Beta;
import org.yaml.snakeyaml.nodes.Tag;

import java.util.stream.IntStream;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Restricted(Beta.class)
public final class Scalar implements CNode, CharSequence {

    private String value;
    private Tag tag;
    private boolean raw;
    private Source source;

    public Scalar(String value, Source source) {
        this(value);
        this.source = source;
    }

    public Scalar(String value) {
        this.value = value;
        this.tag = Tag.STR;
        this.raw = false;
    }

    public Scalar(Enum instance) {
        this.value = instance.name();
        this.tag = Tag.STR;
        this.raw = true;
    }

    public Scalar(Boolean instance) {
        this.value = String.valueOf(instance);
        this.tag = Tag.BOOL;
        this.raw = true;
    }

    public Scalar(Number instance) {
        this.value = String.valueOf(instance);
        this.tag = Tag.INT;
        this.raw = true;
    }

    @Override
    public Type getType() {
        return Type.SCALAR;
    }

    public Tag getTag() {
        return tag;
    }

    public boolean isRaw() {
        return raw;
    }

    @Override
    public Scalar asScalar() {
        return this;
    }

    public String getValue() {
        return value;
    }

    public boolean isSensitiveData() {
        return SecretSource.requiresReveal(value).isPresent();
    }

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

}
