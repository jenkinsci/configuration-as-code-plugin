package org.jenkinsci.plugins.casc.model;

import java.util.HashMap;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public final class Mapping extends HashMap<String, CNode> implements CNode {

    public static final Mapping EMPTY = new Mapping();

    @Override
    public Type getType() {
        return Type.MAPPING;
    }

    @Override
    public Mapping asMapping() {
        return this;
    }
}
