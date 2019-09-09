package io.jenkins.plugins.casc.impl.attributes;

import io.jenkins.plugins.casc.Attribute;
import java.util.Collection;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */

public class MultivaluedAttribute<Owner, Type> extends Attribute<Owner, Collection<Type>> {


    public MultivaluedAttribute(String name, Class type) {
        super(name, type);
        this.multiple = true;
    }

}
