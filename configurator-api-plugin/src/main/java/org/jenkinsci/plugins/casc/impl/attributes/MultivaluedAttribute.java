package org.jenkinsci.plugins.casc.impl.attributes;

import org.jenkinsci.plugins.casc.Attribute;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.Beta;

import java.util.Collection;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Restricted(Beta.class)
public class MultivaluedAttribute<Owner, Type> extends Attribute<Owner, Collection<Type>> {


    public MultivaluedAttribute(String name, Class type) {
        super(name, type);
        this.multiple = true;
    }

}
