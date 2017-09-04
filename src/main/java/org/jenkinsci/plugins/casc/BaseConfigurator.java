package org.jenkinsci.plugins.casc;

import hudson.model.Describable;
import org.apache.commons.beanutils.PropertyUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.accmod.Restricted;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public abstract class BaseConfigurator<T> extends Configurator<T> {

    public Set<Attribute> describe() {

        Set<Attribute> attributes = new HashSet<>();

        final PropertyDescriptor[] properties = PropertyUtils.getPropertyDescriptors(getTarget());
        for (PropertyDescriptor p : properties) {
            final String name = p.getName();

            final Method setter = p.getWriteMethod();
            if (setter == null) continue; // read only
            if (setter.getAnnotation(Deprecated.class) != null) continue; // not actually public
            if (setter.getAnnotation(Restricted.class) != null) continue; // not actually public     - require access-modifier 1.12

            Class c;
            final Type type = setter.getGenericParameterTypes()[0];
            if (type instanceof ParameterizedType) {
                // List<? extends Foo>
                ParameterizedType pt = (ParameterizedType) type;
                c = (Class) pt.getRawType();
            } else {
                c = (Class) type;
            }

            boolean multiple = false;
            if (Collection.class.isAssignableFrom(c)) {
                multiple = true;
                ParameterizedType pt = (ParameterizedType) type;
                Type actualType = pt.getActualTypeArguments()[0];
                if (actualType instanceof WildcardType) {
                    actualType = ((WildcardType) actualType).getUpperBounds()[0];
                }
                if (!(actualType instanceof Class)) {
                    throw new IllegalStateException("Can't handle "+type);
                }
                c = (Class) actualType;
            }

            Attribute attribute;
            if (Describable.class.isAssignableFrom(c)) {
                attribute = new DescribableAttribute(p.getName(), c);
            } else {
                attribute = new Attribute(p.getName(), c);
            }
            attributes.add(attribute.withMultiple(multiple));

            // See https://github.com/jenkinsci/structs-plugin/pull/18
            final Symbol s = setter.getAnnotation(Symbol.class);
            // TODO record symbol as preferred name / alias for this attribute
        }
        return attributes;
    }
}
