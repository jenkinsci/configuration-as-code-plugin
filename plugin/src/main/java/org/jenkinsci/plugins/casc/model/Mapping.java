package org.jenkinsci.plugins.casc.model;

import org.jenkinsci.plugins.casc.ConfiguratorException;

import java.util.HashMap;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public final class Mapping extends HashMap<String, CNode> implements CNode {

    public static final Mapping EMPTY = new Mapping();
    private Source source;

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

    public void putIfNotNull(String key, CNode node) {
        if (node != null) super.put(key, node);
    }

    public void putIfNotEmpry(String key, Sequence seq) {
        if (!seq.isEmpty()) super.put(key, seq);
    }

    public String getScalarValue(String key) throws ConfiguratorException {
        return get(key).asScalar().getValue();
    }

    public void setSource(Source source) {
        this.source = source;
    }

    @Override
    public Source getSource() {
        return source;
    }
}
