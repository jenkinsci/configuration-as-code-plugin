package org.jenkinsci.plugins.casc.model;

import org.jenkinsci.plugins.casc.SecretSource;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public final class Scalar implements CNode, CharSequence {

    private String value;

    public Scalar(String value) {
        this.value = value;
    }

    @Override
    public Type getType() {
        return Type.SCALAR;
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
