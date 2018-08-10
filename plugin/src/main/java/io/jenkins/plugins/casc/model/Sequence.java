package io.jenkins.plugins.casc.model;

import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.Beta;

import java.util.ArrayList;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Restricted(Beta.class)
public final class Sequence extends ArrayList<CNode> implements CNode {

    private Source source;

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

    public void setSource(Source source) {
        this.source = source;
    }

    @Override
    public Source getSource() {
        return source;
    }

    @Override
    public Sequence clone() {
        final Sequence clone = new Sequence();
        stream().map(CNode::clone).forEach(clone::add);
        return clone;
    }
}
