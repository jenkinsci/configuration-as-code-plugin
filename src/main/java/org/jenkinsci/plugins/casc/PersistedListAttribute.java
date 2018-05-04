package org.jenkinsci.plugins.casc;

import hudson.util.PersistedList;

import java.util.Collection;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class PersistedListAttribute<T> extends Attribute<T> {

    private final PersistedList<T> target;

    public PersistedListAttribute(String name, PersistedList<T> target, Class<T> type) {
        super(name, type);
        multiple(true);
        this.target = target;
    }

    @Override
    public void setValue(T t, Object o) throws Exception {
        Collection values = (Collection) o;
        target.replaceBy(values);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PersistedListAttribute<?> pla = (PersistedListAttribute<?>) o;
        return pla.target == target;
    }

    @Override
    public int hashCode() {
        return target.hashCode();
    }
}
