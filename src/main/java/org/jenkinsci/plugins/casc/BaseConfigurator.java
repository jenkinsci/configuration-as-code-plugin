package org.jenkinsci.plugins.casc;

import hudson.model.Describable;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.stapler.export.Exported;

import java.beans.PropertyDescriptor;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * {@link Configurator} that uses Java Beans pattern to the target object.
 *
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public abstract class BaseConfigurator<T> extends Configurator<T> {

    private static final Logger LOGGER = Logger.getLogger(BaseConfigurator.class.getName());

    public Set<Attribute> describe() {

        Set<Attribute> attributes = new HashSet<>();

        final Class<T> target = getTarget();
        final PropertyDescriptor[] properties = PropertyUtils.getPropertyDescriptors(target);
        LOGGER.log(Level.FINE, "Found {0} properties for {1}", new Object[]{properties.length, target});
        for (PropertyDescriptor p : properties) {
            final String name = p.getName();
            LOGGER.log(Level.FINER, "Processing {0} property", name);

            final Method setter = p.getWriteMethod();
            if (setter == null) {
                LOGGER.log(Level.FINE, "Ignored {0} property: read only", name);
                continue; // read only
            }
            if (setter.getAnnotation(Deprecated.class) != null) {
                LOGGER.log(Level.FINE, "Ignored {0} property: deprecated", name);
                continue; // not actually public
            }
            if (setter.getAnnotation(Restricted.class) != null) {
                LOGGER.log(Level.FINE, "Ignored {0} property: restricted", name);
                continue; // not actually public     - require access-modifier 1.12
            }

            // FIXME move this all into cleaner logic to discover property type
            Type type = setter.getGenericParameterTypes()[0];
            Attribute attribute = detectActualType(name, type);
            if (attribute == null) continue;
            attributes.add(attribute);

            final Method getter = p.getReadMethod();
            if (getter != null) {
                final Exported annotation = getter.getAnnotation(Exported.class);
                if (annotation != null && isNotBlank(annotation.name())) {
                    attribute.preferredName(annotation.name());
                }
            }

            // See https://github.com/jenkinsci/structs-plugin/pull/18
            final Symbol s = setter.getAnnotation(Symbol.class);
            if (s != null) {
                attribute.preferredName(s.value()[0]);
            }
        }
        return attributes;
    }

    protected Attribute detectActualType(String name, Type type) {
        Class c = null;
        boolean multiple = false;

        if (type instanceof GenericArrayType) {
            // type is a parameterized array: <Foo>[]
            multiple = true;
            GenericArrayType at = (GenericArrayType) type;
            type = at.getGenericComponentType();
        }
        while (type instanceof ParameterizedType) {
            // type is parameterized `Some<Foo>`
            ParameterizedType pt = (ParameterizedType) type;

            Class rawType = (Class) pt.getRawType();
            if (Collection.class.isAssignableFrom(rawType)) {
                // type is `Collection<Foo>`
                multiple = true;
            }

            type = pt.getActualTypeArguments()[0];
            if (type instanceof WildcardType) {
                // pt is Some<? extends Foo>
                Type t = ((WildcardType) type).getUpperBounds()[0];
                if (t == Object.class) {
                    // pt is Some<?>, so we actually want "Some"
                    type = pt.getRawType();
                } else {
                    type = t;
                }
            }
        }

        if (type instanceof ParameterizedType) {
            final Type[] arguments = ((ParameterizedType) type).getActualTypeArguments();
            type = ((ParameterizedType) type).getRawType();
        }

        while (c == null) {
            if (type instanceof Class) {
                c = (Class) type;
            } else if (type instanceof TypeVariable) {

                // type is declared as parameterized type
                // unfortunately, java reflection doesn't allow to get the actual parameter type
                // so, if superclass it parameterized, we assume parameter type match
                // i.e target is Foo extends AbtractFoo<Bar> with
                // public abstract class AbtractFoo<T> { void setBar(T bar) }
                final Type superclass = getTarget().getGenericSuperclass();
                if (superclass instanceof ParameterizedType) {
                    final ParameterizedType psc = (ParameterizedType) superclass;
                    type = psc.getActualTypeArguments()[0];
                    continue;
                } else {
                    c = (Class) ((TypeVariable) type).getBounds()[0];
                }

                TypeVariable tv = (TypeVariable) type;
            } else {
                throw new IllegalStateException("Unable to detect type of attribute " + getTarget() + '#' + name);
            }
        }

        if (c.isArray()) {
            multiple = true;
            c = c.getComponentType();
        }

        Attribute attribute;
        if (!c.isPrimitive() && !c.isEnum() && Modifier.isAbstract(c.getModifiers())) {
            if (!Describable.class.isAssignableFrom(c)) {
                // Not a Describable, so probably not an attribute expected to be selected as sub-component
                return null;
            }
            attribute = new DescribableAttribute<T>(name, c);
        } else {
            attribute = new Attribute<T>(name, c);
        }
        attribute.multiple(multiple);
        return attribute;
    }

    protected void configure(Map config, T instance) throws Exception {
        final Set<Attribute> attributes = describe();

        for (Attribute attribute : attributes) {
            final String name = attribute.getName();
            final Object sub = removeIgnoreCase(config, name);
            if (sub != null) {
                final Class k = attribute.getType();
                final Configurator configurator = Configurator.lookup(k);
                if (configurator == null) throw new IllegalStateException("No configurator implementation to manage "+k);

                final Object valueToSet;
                if (attribute.isMultiple()) {
                    List values = new ArrayList<>();
                    for (Object o : (List) sub) {
                        Object value = configurator.configure(o);
                        values.add(value);
                    }
                    valueToSet= values;
                } else {
                    valueToSet = configurator.configure(sub);
                }

                try {
                    attribute.setValue(instance, valueToSet);
                } catch (Exception ex) {
                    throw new Exception("Failed to set attribute " + attribute, ex);
                }
            }
        }
        if (!config.isEmpty()) {
            final String invalid = StringUtils.join(config.keySet(), ',');
            throw new IllegalArgumentException("Invalid configuration elements for type " + instance.getClass() + " : " + invalid);
        }
    }

    private Object removeIgnoreCase(Map config, String name) {
        for (Object k : config.keySet()) {
            if (name.equalsIgnoreCase(k.toString())) {
                return config.remove(k);
            }
        }
        return null;
    }
}
