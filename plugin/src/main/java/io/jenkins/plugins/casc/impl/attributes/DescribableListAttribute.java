package io.jenkins.plugins.casc.impl.attributes;

import hudson.util.PersistedList;
import java.util.Collection;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class DescribableListAttribute<Owner, Type> extends DescribableAttribute<Owner, Collection<Type>> {

    public DescribableListAttribute(String name, Class type) {
        super(name, type);
        multiple(true);
    }

    @Override
    public void setValue(Owner target, Collection<Type> o) throws Exception {
        getValue(target).replaceBy(o);
    }

    @Override
    public PersistedList getValue(Owner o) throws Exception {
        return (PersistedList) super.getValue(o);
    }
}
