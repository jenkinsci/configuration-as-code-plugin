package org.jenkinsci.plugins.casc;

import java.util.Collection;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class MultivaluedAttribute<T,O> extends Attribute<Collection<T>, O> {


    public MultivaluedAttribute(String name, Class type) {
        super(name, type);
        this.multiple = true;
    }

}
