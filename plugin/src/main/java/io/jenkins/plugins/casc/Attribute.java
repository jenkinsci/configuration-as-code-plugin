package io.jenkins.plugins.casc;

import hudson.util.Secret;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Scalar;
import io.jenkins.plugins.casc.model.Sequence;
import io.jenkins.plugins.casc.util.ExtraFieldUtils;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.accmod.AccessRestriction;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.export.Exported;

import static io.jenkins.plugins.casc.Blacklist.isBlacklisted;
import static io.jenkins.plugins.casc.ConfigurationAsCode.printThrowable;

/**
 * One attribute of {@link Configurator}.
 *
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 * @see Configurator#describe()
 */

public class Attribute<Owner, Type> {

    private static final Logger LOGGER = Logger.getLogger(Attribute.class.getName());
    private static final Class[] EMPTY = new Class[0];

    /** For pseudo-attributes which are actually managed directly as singletons, not set on some owner component */
    private static final Setter NOOP = (target, value) -> {
        // Nop
    };

    //TODO: Concurrent cache?
    //private static final HashMap<Class, Boolean> SECRET_ATTRIBUTE_CACHE =
    //        new HashMap<>();

    protected final String name;
    protected final Class type;
    protected boolean multiple;
    protected String preferredName;
    private Setter<Owner, Type> setter;
    private Getter<Owner, Type> getter;
    private boolean secret;
    private boolean isJsonSchema;

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
        this.secret = type == Secret.class || calculateIfSecret(null, this.name);
    }

    public Attribute(List<String> name, Class type) {
        this.name = name.get(0);
        this.type = type;
        this.getter = this::_getValue;
        this.setter = this::_setValue;
        this.aliases = name;
        this.secret = type == Secret.class || calculateIfSecret(null, this.name);
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

    boolean isIgnored() {
        return isDeprecated() || isRestricted() || isBlacklisted(this);
    }

    public Class<? extends AccessRestriction>[] getRestrictions() {
        return restrictions != null ? restrictions : EMPTY;
    }

    /**
     * Set jsonSchema is used to tell the describe function to call the describe structure
     * so that it supports and returns a nested structure
     */
    public void setJsonSchema(boolean jsonSchema) {
        isJsonSchema = jsonSchema;
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

    /**
     * Sets whether the attribute is secret.
     * If so, various outputs will be suppressed (exports, logging).
     * @param secret {@code true} to make an attribute secret
     * @since 1.25
     */
    public Attribute<Owner, Type> secret(boolean secret) {
        this.secret = secret;
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

    /**
     * Checks whether an attribute is considered a secret one.
     * @return {@code true} if the attribute is secret
     * @param target Target object.
     *               If {@code null}, only the attribute metadata is checked
     * @since 1.25
     */
    public boolean isSecret(@CheckForNull Owner target) {
        // This.secret should be always true for the first condition, but getType() is overridable
        // Here we define an additional check just in case getType() is overridden in another implementation
        return secret || calculateIfSecret(target != null ? target.getClass() : null, this.name);
    }

    public void setValue(Owner target, Type value) throws Exception {
        LOGGER.log(Level.FINE, "Setting {0}.{1} = {2}",
                new Object[] {target, name, (isSecret(target) ? "****" : value)});
        setter.setValue(target, value);
    }

    public Type getValue(Owner target) throws Exception {
        return getter.getValue(target);
    }

    public CNode describe(Owner instance, ConfigurationContext context) throws ConfiguratorException {
        final Configurator c = context.lookup(type);
        if (c == null) {
            return new Scalar("FAILED TO EXPORT\n" + instance.getClass().getName()+"#"+name +
                    ": No configurator found for type " + type);
        }
        try {
            Object o = getValue(instance);
            if (o == null) {
                return null;
            }

            // In Export we sensitive only those values which do not get rendered as secrets
            boolean shouldBeMasked = isSecret(instance);
            if (multiple) {
                Sequence seq = new Sequence();
                if (o.getClass().isArray()) o = Arrays.asList((Object[]) o);
                if (o instanceof Iterable) {
                    for (Object value : (Iterable) o) {
                        seq.add(_describe(c, context, value, shouldBeMasked));
                    }
                } else {
                    LOGGER.log(Level.FINE, o.getClass().toString() + " is not iterable");
                }
                return seq;
            }
            return _describe(c, context, o, shouldBeMasked);
        } catch (Exception | /* Jenkins.getDescriptorOrDie */AssertionError e) {
            // Don't fail the whole export, prefer logging this error
            LOGGER.log(Level.WARNING, "Failed to export", e);
            return new Scalar("FAILED TO EXPORT\n" + instance.getClass().getName() + "#" + name + ": "
                + printThrowable(e));
        }
    }

    /**
     * This function is for the JSONSchemaGeneration
     * @param instance Owner Instance
     * @param context Context to be passed
     * @return CNode object describing the structure of the node
     */
    public CNode describeForSchema (Owner instance, ConfigurationContext context) {
        final Configurator c = context.lookup(type);
        if (c == null) {
            return new Scalar("FAILED TO EXPORT\n" + instance.getClass().getName()+"#"+name +
                ": No configurator found for type " + type);
        }
        try {
            Object o = getType();
            if (o == null) {
                return null;
            }

            // In Export we sensitive only those values which do not get rendered as secrets
            boolean shouldBeMasked = isSecret(instance);
            if (multiple) {
                Sequence seq = new Sequence();
                if (o.getClass().isArray()) o = Arrays.asList(o);
                if (o instanceof Iterable) {
                    for (Object value : (Iterable) o) {
                        seq.add(_describe(c, context, value, shouldBeMasked));
                    }
                }
                return seq;
            }
            return _describe(c, context, o, shouldBeMasked);
        } catch (Exception e) {
            // Don't fail the whole export, prefer logging this error
            LOGGER.log(Level.WARNING, "Failed to export", e);
            return new Scalar("FAILED TO EXPORT\n" + instance.getClass().getName() + "#" + name + ": "
                + printThrowable(e));
        }
    }

    /**
     * Describes a node.
     * @param c Configurator
     * @param context Context to be passed
     * @param value Value
     * @param shouldBeMasked If {@code true}, the value should be masked in the output.
     *                       It will be applied to {@link Scalar} nodes only.
     * @throws Exception export error
     * @return Node
     */
    private CNode _describe(Configurator c, ConfigurationContext context, Object value, boolean shouldBeMasked)
            throws Exception {
        CNode node;
        if (isJsonSchema) {
            node = c.describeStructure(value, context);
        } else {
            node = c.describe(value, context);
        }
        if (shouldBeMasked && node instanceof Scalar) {
            ((Scalar)node).sensitive(true);
        }
        return node;
    }

    public boolean equals(Owner o1, Owner o2) throws Exception {
        final Object v1 = getValue(o1);
        final Object v2 = getValue(o2);
        if (v1 == null && v2 == null) return true;
        if (multiple && v1 instanceof Collection && v2 instanceof Collection) {
            Collection c1 = (Collection) v1;
            Collection c2 = (Collection) v2;
            return CollectionUtils.isEqualCollection(c1, c2);
        }
        return v1 != null && v1.equals(v2);
    }

    /**
     * Abstracts away how to assign a value to a 'target' Jenkins object.
     */
    @FunctionalInterface
    public interface Setter<O,T> {
        Setter NOP =  (o,v) -> {};
        void setValue(O target, T value) throws Exception;
    }

    /**
     * Abstracts away how to retrieve attribute value from a 'target' Jenkins object.
     */
    @FunctionalInterface
    public interface Getter<O,T> {
        T getValue(O target) throws Exception;
    }

    @CheckForNull
    private static Method locateGetter(Class<?> clazz, @Nonnull String fieldName) {
        final String upname = StringUtils.capitalize(fieldName);
        final List<String> accessors = Arrays.asList("get" + upname, "is" + upname);

        for (Method method : clazz.getMethods()) {
            if (method.getParameterCount() != 0) continue;
            if (accessors.contains(method.getName())) {
                return method;
            }

            final Exported exported = method.getAnnotation(Exported.class);
            if (exported != null && exported.name().equalsIgnoreCase(fieldName)) {
                return method;
            }
        }
        return null;
    }

    @CheckForNull
    private static Field locatePublicField(Class<?> clazz, @Nonnull String fieldName) {
        return ExtraFieldUtils.getField(clazz, fieldName, false);
    }

    @CheckForNull
    private static Field locatePrivateFieldInHierarchy(Class<?> clazz, @Nonnull String fieldName) {
        return ExtraFieldUtils.getFieldNoForce(clazz, fieldName);
    }

    //TODO: consider Boolean and third condition
    /**
     * This is a method which tries to guess whether an attribute is {@link Secret}.
     * @param targetClass Class of the target object. {@code null} if unknown
     * @param fieldName Field name
     * @return {@code true} if the attribute is secret
     *         {@code false} if not or if there is no conclusive answer.
     */
    @Restricted(NoExternalUse.class)
    public static boolean calculateIfSecret(@CheckForNull Class<?> targetClass, @Nonnull String fieldName) {
        if (targetClass == Secret.class) { // Class is final, so the check is safe
            LOGGER.log(Level.FINER, "Attribute {0}#{1} is secret, because it has a Secret type",
                    new Object[] {targetClass.getName(), fieldName});
            return true;
        }

        if (targetClass == null) {
            LOGGER.log(Level.FINER, "Attribute {0} is assumed to be non-secret, because there is no class instance in the call. " +
                            "This call is used only for fast-fetch caching, and the result may be adjusted later",
                    new Object[] {fieldName});
            return false; // All methods below require a known target class
        }

        //TODO: Cache decisions?

        Method m = locateGetter(targetClass, fieldName);
        if (m != null && m.getReturnType() == Secret.class) {
            LOGGER.log(Level.FINER, "Attribute {0}#{1} is secret, because there is a getter {2} which returns a Secret type",
                    new Object[] {targetClass.getName(), fieldName, m});
            return true;
        }

        Field f = locatePublicField(targetClass, fieldName);
        if (f != null && f.getType() == Secret.class) {
            LOGGER.log(Level.FINER, "Attribute {0}#{1} is secret, because there is a public field {2} which has a Secret type",
                    new Object[] {targetClass.getName(), fieldName, f});
            return true;
        }

        f = locatePrivateFieldInHierarchy(targetClass, fieldName);
        if (f != null && f.getType() == Secret.class) {
            LOGGER.log(Level.FINER, "Attribute {0}#{1} is secret, because there is a private field {2} which has a Secret type",
                    new Object[] {targetClass.getName(), fieldName, f});
            return true;
        }

        // TODO(oleg_nenashev): Consider setters? Gonna be more interesting since there might be many of them
        LOGGER.log(Level.FINER, "Attribute {0}#{1} is not a secret, because all checks have passed",
                new Object[] {targetClass.getName(), fieldName});
        return false;
    }

    private Type _getValue(Owner target) throws ConfiguratorException {
        final Class<?> clazz = target.getClass();

        try {
            final Method method = locateGetter(clazz, this.name);
            if (method != null) {
                return (Type) method.invoke(target);
            }

            // If this is a public final field, developers don't define getters as jelly can use them as-is
            final Field field = ExtraFieldUtils.getField(clazz, this.name, true);
            if (field != null) {
                return (Type) field.get(target);
            }

            throw new ConfiguratorException("Can't read attribute '" + name + "' from "+ target);
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
            // Find most specialized variant of setter because the method
            // can to have been overridden with concretized type
            if (method.getName().equals("set" + StringUtils.capitalize(name)) && (
                writeMethod == null
                    || writeMethod.getParameterTypes()[0]
                    .isAssignableFrom(method.getParameterTypes()[0]))) {
                writeMethod = method;
            }
        }

        if (writeMethod == null) {
            throw new IllegalStateException(
                "Default value setter cannot find Property Descriptor for " + setterId);
        }

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

    public static final <T,V> Setter<T,V> noop() {
        return NOOP;
    }

}
