package org.jenkinsci.plugins.systemconfigdsl;

import org.jvnet.tiger_types.Types;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author Kohsuke Kawaguchi
 */
public class Property {
    private final String name;
    private final Type type;

    private boolean collection;
    private Class itemType;

    private Object value;

    public Property(String name, Type type) {
        this.name = name;
        this.type = type;

        if (Types.isArray(type)) {
            collection = true;
            itemType = Types.erasure(Types.getComponentType(type));
        } else
        if (Types.isSubClassOf(type,Collection.class)) {
            Type assignment = Types.getBaseClass(type, Collection.class);
            collection = true;
            itemType = Types.erasure(Types.getTypeArgument(assignment,0));
        } else {
            collection = false;
            itemType = Types.erasure(type);
        }

        if (collection)
            value = new ArrayList();
    }

    public void add(Object o) {
        if (o!=null && !itemType.isInstance(o))
            throw new IllegalArgumentException(String.format("Expected %s but got %s instead",
                    itemType.getName(), o.getClass().getName()));

        if (collection) {
            if (o instanceof Collection) {
                // bulk addition
                ((List) value).addAll((Collection) o);
            } else {
                ((List) value).add(o);
            }
        } else {
            value = o;
        }
    }

    /**
     * Based on accumulated value, produce a value for this property.
     */
    public Object pack() {
        if (collection) {
            List l = (List)value;
            if (Types.isArray(type)) {
                Object[] o = (Object[]) Array.newInstance(itemType, l.size());
                l.toArray(o);
                return o;
            }
        }
        return value;
    }
}
