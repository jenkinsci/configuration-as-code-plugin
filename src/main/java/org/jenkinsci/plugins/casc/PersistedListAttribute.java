package org.jenkinsci.plugins.casc;

import hudson.util.PersistedList;

import java.util.Collection;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class PersistedListAttribute<T> extends Attribute<PersistedList<T>> {

    public PersistedListAttribute(String name, Class type) {
        super(name, type);
        multiple(true);
    }

    @Override
    public void setValue(PersistedList target, Object o) throws Exception {
        Collection values = (Collection) o;
        target.replaceBy(values);
    }
}
