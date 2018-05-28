package org.jenkinsci.plugins.casc.model;

import org.jenkinsci.plugins.casc.ConfiguratorException;

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


    public void put(String key, String value) {
        super.put(key, new Scalar(value));
    }

    public void put(String key, Number value) {
        super.put(key, new Scalar(String.valueOf(value)));
    }

    public void put(String key, Boolean value) {
        super.put(key, new Scalar(String.valueOf(value)));
    }

    public String getScalarValue(String key) throws ConfiguratorException {
        return get(key).asScalar().getValue();
    }
}
