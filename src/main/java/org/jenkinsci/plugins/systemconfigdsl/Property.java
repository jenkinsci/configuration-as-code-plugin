package org.jenkinsci.plugins.systemconfigdsl;

import com.thoughtworks.xstream.core.util.Primitives;
import org.jvnet.tiger_types.Types;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Represents assignable property of an object in Jenkins.
 *
 * @author Kohsuke Kawaguchi
 */
final class Property {
    /**
     * Name of the property.
     */
    final String name;
    /**
     * Type of the property such as String or {@code List<Describable>}
     */
    final Type type;

    /**
     * If {@link #type} is a collection of a value (such as List or array), true.
     */
    final boolean collection;
    /**
     * If this is a collection property, type of the individual
     */
    final Class itemType;

    /**
     * Abstracts away how to assign the property. Null if this is representing a parameter to constructor.
     */
    final Setter setter;

    private Object value;

    private boolean set;

    public Property(String name, Type type, Setter setter) {
        this.name = name;
        this.type = type;
        this.setter = setter;

        if (Types.isArray(type)) {
            collection = true;
            itemType = box(Types.erasure(Types.getComponentType(type)));
        } else
        if (Types.isSubClassOf(type,Collection.class)) {
            Type assignment = Types.getBaseClass(type, Collection.class);
            collection = true;
            itemType = box(Types.erasure(Types.getTypeArgument(assignment, 0, Object.class)));
        } else {
            collection = false;
            itemType = box(Types.erasure(type));
        }

        if (collection)
            value = new ArrayList();
    }

    private Class box(Class t) {
        Class b = Primitives.box(t);
        return b!=null ? b : t;
    }

    public void add(Object o) {
        set = true;
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

    public void assignTo(Object instance) {
        if (!set)           return; // no value assigned to this property
        if (setter==null)   return; // meant to be used with constructor
        try {
            setter.set(instance,pack());
        } catch (Exception e) {
            // TODO: chain source location
            throw new RuntimeException(e);
        }
    }
}
