package org.jenkinsci.plugins.casc;

import hudson.model.Describable;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.casc.model.CNode;
import org.jenkinsci.plugins.casc.model.Mapping;
import org.kohsuke.accmod.AccessRestriction;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.Beta;
import org.kohsuke.accmod.restrictions.None;
import org.kohsuke.stapler.export.Exported;

import java.beans.Introspector;
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
import java.util.HashMap;
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
        for (Method method : target.getMethods()) {
            if (method.getParameterCount() != 1 || !method.getName().startsWith("set")) continue;

            final String sm = method.getName().substring(3);
            final String name = StringUtils.uncapitalize(sm);
            LOGGER.log(Level.FINER, "Processing {0} property", name);

            // FIXME move this all into cleaner logic to discover property type
            Type type = method.getGenericParameterTypes()[0];
            Attribute attribute = detectActualType(name, type);
            if (attribute == null) continue;
            attributes.add(attribute);

            attribute.deprecated(method.getAnnotation(Deprecated.class) != null);
            final Restricted r = method.getAnnotation(Restricted.class);
            if (r != null) attribute.restrictions(r.value());
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
            attribute = new DescribableAttribute(name, c);
        } else {
            attribute = new Attribute(name, c);
        }
        attribute.multiple(multiple);
        return attribute;
    }

    protected void configure(Mapping config, T instance) throws ConfiguratorException {
        final Set<Attribute> attributes = describe();
        final ConfigurationAsCode casc = ConfigurationAsCode.get();

        for (Attribute<T,Object> attribute : attributes) {

            final String name = attribute.getName();
            CNode sub = removeIgnoreCase(config, name);
            if (sub == null) {
                for (String alias : attribute.aliases) {
                    sub = removeIgnoreCase(config, alias);
                    if (sub != null) {
                        ObsoleteConfigurationMonitor.get().record(sub, "'"+alias+"' is an obsolete attribute name, please use '" + name + "'");
                        break;
                    }
                }
            }

            if (sub != null) {

                if (attribute.isDeprecated()) {
                    ObsoleteConfigurationMonitor.get().record(config, "'"+attribute.getName()+"' is deprecated");
                    if (casc.getDeprecation() == ConfigurationAsCode.Deprecation.reject) {
                        throw new ConfiguratorException("'"+attribute.getName()+"' is deprecated");
                    }
                }

                for (Class<? extends AccessRestriction> r : attribute.getRestrictions()) {
                    if (r == None.class) continue;
                    if (r == Beta.class && casc.getRestricted() == ConfigurationAsCode.Restricted.beta) {
                        continue;
                    }
                    ObsoleteConfigurationMonitor.get().record(config, "'"+attribute.getName()+"' is restricted: " + r.getSimpleName());
                    if (casc.getRestricted() == ConfigurationAsCode.Restricted.reject) {
                        throw new ConfiguratorException("'"+attribute.getName()+"' is restricted: " + r.getSimpleName());
                    }
                }

                final Class k = attribute.getType();
                final Configurator configurator = Configurator.lookupOrFail(k);

                final Object valueToSet;
                if (attribute.isMultiple()) {
                    List<Object> values = new ArrayList<>();
                    for (CNode o : sub.asSequence()) {
                        Object value = configurator.configure(o);
                        values.add(value);
                    }
                    valueToSet= values;
                } else {
                    valueToSet = configurator.configure(sub);
                }

                try {
                    LOGGER.info("Setting " + instance + '.' + name + " = " + (sub.isSensitiveData() ? "****" : valueToSet));
                    attribute.setValue(instance, valueToSet);
                } catch (Exception ex) {
                    throw new ConfiguratorException(configurator, "Failed to set attribute " + attribute, ex);
                }
            }
        }
        if (!config.isEmpty()) {
            final String invalid = StringUtils.join(config.keySet(), ',');
            throw new ConfiguratorException("Invalid configuration elements for type " + instance.getClass() + " : " + invalid);
        }
    }

    protected Mapping compare(T o1, T o2) throws Exception {

        Mapping mapping = new Mapping();
        for (Attribute attribute : describe()) {
            if (attribute.equals(o1, o2)) continue;
            mapping.put(attribute.getName(), attribute.describe(o1));
        }
        return mapping;
    }

    private CNode removeIgnoreCase(Mapping config, String name) {
        for (String k : config.keySet()) {
            if (name.equalsIgnoreCase(k)) {
                return config.remove(k);
            }
        }
        return null;
    }
}
