package org.jenkinsci.plugins.casc.model;

import org.jenkinsci.plugins.casc.SecretSource;
import org.yaml.snakeyaml.nodes.Tag;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public final class Scalar implements CNode, CharSequence {

    private String value;
    private Tag tag;
    private boolean raw;

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



    @Override
    public String toString() {
        String s = value;
        Optional<String> r = SecretSource.requiresReveal(value);
        if(r.isPresent()) {
            Optional<String> reveal = Optional.empty();
            for (SecretSource secretSource : SecretSource.all()) {
                try {
                    reveal = secretSource.reveal(r.get());
                } catch (IOException ex) {
                    throw new RuntimeException("Cannot reveal secret source for variable with key: " + s, ex);
                }
                if(reveal.isPresent()) {
                    s = reveal.get();
                    break;
                }
            }
            if(!reveal.isPresent()) {
                throw new RuntimeException("Unable to reveal variable with key: "+s);
            }
        }
        return s;
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
}
