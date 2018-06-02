package org.jenkinsci.plugins.casc.model;

import java.util.ArrayList;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public final class Sequence extends ArrayList<CNode> implements CNode {

    private String source;

    public Sequence() {
    }

    public Sequence(int initialCapacity) {
        super(initialCapacity);
    }

    @Override
    public Type getType() {
        return Type.SEQUENCE;
    }


    @Override
    public Sequence asSequence() {
        return this;
    }

    public void setSource(String source) {
        this.source = source;
    }

    @Override
    public String source() {
        return source;
    }
}
