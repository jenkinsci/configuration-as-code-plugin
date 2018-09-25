package io.jenkins.plugins.casc;

import hudson.Functions;
import hudson.util.Secret;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Scalar;
import io.jenkins.plugins.casc.model.Sequence;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.reflect.FieldUtils;
import org.kohsuke.accmod.AccessRestriction;
import org.kohsuke.stapler.export.Exported;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * One attribute of {@link Configurator}.
 *
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 * @see Configurator#describe()
 */

public class Attribute<Owner, Type> {

    private final static Logger logger = Logger.getLogger(Attribute.class.getName());

    protected final String name;
    protected final Class type;
    protected boolean multiple;
    protected String preferredName;
    private Setter<Owner, Type> setter;
    private Getter<Owner, Type> getter;

    private boolean deprecated;

    private Class<? extends AccessRestriction>[] restrictions;

    protected List<String> aliases;

    public Attribute(String name, Class type) {
        this.name = name;
        this.type = type;
        this.getter = this::_getValue;
        this.setter = this::_setValue;
        this.aliases = new ArrayList<>();
        this.aliases.add(name);
    }

    @SuppressWarnings("unchecked")
    public static <O,T> Optional<Attribute<O,T>> get(Set<Attribute<O,?>> attributes, String name) {
        return attributes.stream()
                .filter(a -> a.name.equals(name))
                .map(a -> (Attribute<O,T>) a)
                .findFirst();
    }

    @Override
    public String toString() {
        return String.format("%s(class: %s, multiple: %s)", name, type, multiple);
    }

    public String getName() {
        return preferredName != null ? preferredName : name;
    }

    public Class getType() {
        return type;
    }

    public boolean isDeprecated() {
        return deprecated;
    }


    private static final Class[] EMPTY = new Class[0];

    public Class<? extends AccessRestriction>[] getRestrictions() {
        return restrictions != null ? restrictions : EMPTY;
    }

    public boolean isRestricted() {
        return restrictions != null && restrictions.length > 0;
    }

    /**
     * Attribute is actually a Collection of documented type
     * @return boolean indicating if this attribute is a list of multiple items of documented type
     */
    public boolean isMultiple() {
        return multiple;
    }

    public Attribute<Owner, Type> multiple(boolean multiple) {
        this.multiple = multiple;
        return this;
    }

    public Attribute<Owner, Type> preferredName(String preferredName) {
        this.preferredName = preferredName;
        return this;
    }

    public Attribute<Owner, Type> setter(Setter<Owner, Type> setter) {
        this.setter = setter;
        return this;
    }

    public Attribute<Owner, Type> alias(String alias) {
        this.aliases.add(alias);
        return this;
    }

    public Attribute<Owner, Type> getter(Getter<Owner, Type> getter) {
        this.getter = getter;
        return this;
    }

    public Attribute deprecated(boolean deprecated) {
        this.deprecated = deprecated;
        return this;
    }

    public Attribute restrictions(Class<? extends AccessRestriction>[] restrictions) {
        this.restrictions = restrictions.clone();
        return this;
    }


    public Setter<Owner, Type> getSetter() {
        return setter;
    }

    public Getter<Owner, Type> getGetter() {
        return getter;
    }

    public List<String> getAliases() {
        return aliases;
    }

    /**
     * If this attribute is constrained to a limited set of value, here they are
     *
     * @return A list of possible types
     */
    public List<String> possibleValues() {
        if (type.isEnum()) {
            Class<Enum> e = (Class<Enum>) type;
            return Arrays.stream(e.getEnumConstants()).map(Enum::name).collect(Collectors.toList());
        }
        return Collections.EMPTY_LIST;
    }


    public void setValue(Owner target, Type value) throws Exception {
        logger.info("Setting " + target + '.' + name + " = " + (getType() == Secret.class ? "****" : value));
        setter.setValue(target, value);
    }

    public Type getValue(Owner target) throws Exception {
        return getter.getValue(target);
    }

    public CNode describe(Owner instance, ConfigurationContext context) throws ConfiguratorException {
        final Configurator c = context.lookup(type);
        if (c == null) {
            return new Scalar("FAILED TO EXPORT " + instance.getClass().getName()+"#"+name +
                    ": No configurator found for type " + type);
        }
        try {
            Object o = getValue(instance);
            if (o == null) {
                return null;
            }
            if (multiple) {
                Sequence seq = new Sequence();
                if (o.getClass().isArray()) o = Arrays.asList((Object[]) o);
                for (Object value : (Iterable) o) {
                    seq.add(c.describe(value, context));
                }
                return seq;
            }
            return c.describe(o, context);
        } catch (Exception | /* Jenkins.getDescriptorOrDie */AssertionError e) {
            // Don't fail the whole export, prefer logging this error
            return new Scalar("FAILED TO EXPORT " + instance.getClass().getName() + "#" + name + ": \n" + Functions.printThrowable(e));
        }
    }

    public boolean equals(Owner o1, Owner o2) throws Exception {
        final Object v1 = getValue(o1);
        final Object v2 = getValue(o2);
        if (v1 == null && v2 == null) return true;
        if (multiple) {
            // FIXME need to compare collection1  and collection2 contain same elements...
        }
        return (v1 != null && v1.equals(v2));
    }

    /**
     * Abstracts away how to assign a value to a 'target' Jenkins object.
     */
    @FunctionalInterface
    public interface Setter<O,T> {
        void setValue(O target, T value) throws Exception;

        Setter NOP =  (o,v) -> {};
    }

    /**
     * Abstracts away how to retrieve attribute value from a 'target' Jenkins object.
     */
    @FunctionalInterface
    public interface Getter<O,T> {
        T getValue(O target) throws Exception;
    }

    private Type _getValue(Owner target) throws ConfiguratorException {
        try {
            final Class<?> clazz = target.getClass();
            final String upname = StringUtils.capitalize(name);
            final List<String> accessors = Arrays.asList("get" + upname, "is" + upname);

            for (Method method : clazz.getMethods()) {
                if (method.getParameterCount() != 0) continue;
                if (accessors.contains(method.getName())) {
                    return (Type) method.invoke(target);
                }

                final Exported exported = method.getAnnotation(Exported.class);
                if (exported != null && exported.name().equalsIgnoreCase(name)) {
                    return (Type) method.invoke(target);
                }
            }

            // If this is a public final field, developers don't define getters as jelly can use them as-is
            final Field field = FieldUtils.getField(clazz, name, true);
            if (field == null) {
                throw new ConfiguratorException("Can't read attribute '" + name + "' from "+ target);
            }

            return (Type) field.get(target);

        } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
            throw new ConfiguratorException("Can't read attribute '" + name + "' from "+ target, e);
        }
    }

    /**
     * Default Setter implementation based on JavaBean property write method.
     *
     */
    private void _setValue(Owner target, Type value) throws Exception {
        final String setterId = target.getClass().getCanonicalName()+'#'+name;

        Method writeMethod = null;
        for (Method method : target.getClass().getMethods()) {
            if (method.getName().equals("set"+StringUtils.capitalize(name))) {
                // Find most specialized wariant of setter because the method
                // can to have been overridden with concretized type
                if (writeMethod == null
                        || writeMethod.getParameterTypes()[0].isAssignableFrom(method.getParameterTypes()[0])) {
                    writeMethod = method;
                }
            }
        }

        if (writeMethod == null)
            throw new Exception("Default value setter cannot find Property Descriptor for " + setterId);

        Object o = value;
        if (multiple) {
            if (!(value instanceof Collection)) {
                throw new IllegalArgumentException(setterId + " should be a list.");
            }
            // if setter expect an Array, convert Collection to expected array type
            // Typically required for hudson.tools.ToolDescriptor.setInstallations
            // as java varargs unfortunately only supports Arrays, not all Iterable (sic)
            final Class c = writeMethod.getParameterTypes()[0];
            if (c.isArray()) {
                Collection collection = (Collection) value;
                o = collection.toArray((Object[]) Array.newInstance(type, collection.size()));

                // if setter expect a Set, convert Collection to Set
                // see jenkins.agentProtocols
            } else if(c.isAssignableFrom(Set.class)){
                o = new HashSet((Collection)value);
            }
        }

        writeMethod.invoke(target, o);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Attribute<?, ?> attribute = (Attribute<?, ?>) o;
        return Objects.equals(name, attribute.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }


    /** For pseudo-attributes which are actually managed directly as singletons, not set on some owner component */
    private static final Setter NOOP = (target, value) -> {
        // Nop
    };

    public static final <T,V> Setter<T,V> noop() {
        return NOOP;
    }

}
