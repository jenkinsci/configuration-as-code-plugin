package org.jenkinsci.plugins.casc;

import hudson.util.PersistedList;

import java.util.Collection;
import java.util.Collections;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class PersistedListAttribute<T,O> extends Attribute<Collection<T>,O> {

    public PersistedListAttribute(String name, Class<T> type) {
        super(name, type);
        multiple(true);
    }

    @Override
    public void setValue(O target, Collection<T> o) throws Exception {
        getValue(target).replaceBy(o);
    }

    @Override
    public PersistedList getValue(O o) throws Exception {
        return (PersistedList) super.getValue(o);
    }

}
