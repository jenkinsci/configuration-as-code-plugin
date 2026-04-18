package io.jenkins.plugins.casc;

import static java.lang.reflect.Array.newInstance;
import static java.lang.reflect.Array.set;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.BulkChange;
import hudson.model.Describable;
import hudson.model.Saveable;
import hudson.util.DescribableList;
import hudson.util.PersistedList;
import io.jenkins.plugins.casc.impl.attributes.DescribableAttribute;
import io.jenkins.plugins.casc.impl.attributes.DescribableListAttribute;
import io.jenkins.plugins.casc.impl.attributes.PersistedListAttribute;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import io.jenkins.plugins.casc.model.Scalar;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.accmod.AccessRestriction;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.Beta;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.accmod.restrictions.None;

/**
 * a General purpose abstract {@link Configurator} implementation based on introspection.
 * Target component is identified by implementing {@link #instance(Mapping, ConfigurationContext)} then configuration is applied on
 * {@link Attribute}s as defined by {@link #describe()}.
 * This base implementation uses JavaBean convention to identify configurable attributes.
 *
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public abstract class BaseConfigurator<T> implements Configurator<T> {

    private static final Logger LOGGER = Logger.getLogger(BaseConfigurator.class.getName());

    @NonNull
    public Set<Attribute<T, ?>> describe() {

        Map<String, Attribute<T, ?>> attributes = new HashMap<>();
        final Set<String> exclusions = exclusions();

        for (Field field : getTarget().getFields()) {
            final String name = field.getName();
            if (exclusions.contains(name)) {
                continue;
            }

            if (PersistedList.class.isAssignableFrom(field.getType())) {
                if (Modifier.isTransient(field.getModifiers())) {
                    exclusions.add(name);
                    continue;
                }

                Attribute attribute = createAttribute(name, TypePair.of(field))
                        .getter(field::get); // get value by direct access to public final field
                attributes.put(name, attribute);
            }
        }

        final Class<T> target = getTarget();
        Map<String, List<Method>> methodsByProperty = new HashMap<>();
        // Resolve the methods and merging overrides to more concretized signatures
        // because the methods can to have been overridden with concretized type
        for (Method method : target.getMethods()) {
            final String methodName = method.getName();
            if (method.getParameterCount() == 0
                    && methodName.startsWith("get")
                    && PersistedList.class.isAssignableFrom(method.getReturnType())) {
                String name = StringUtils.uncapitalize(methodName.substring(3));
                if (exclusions.contains(name)) {
                    continue;
                }

                TypePair type = TypePair.ofReturnType(method);
                @SuppressWarnings("unchecked")
                Attribute<T, ?> attribute = (Attribute<T, ?>) createAttribute(name, type);

                if (attribute != null) {
                    attribute.deprecated(method.getAnnotation(Deprecated.class) != null);
                    final Restricted r = method.getAnnotation(Restricted.class);
                    if (r != null) {
                        attribute.restrictions(r.value());
                    }
                    attributes.putIfAbsent(name, attribute);
                }
                continue;
            }

            if (method.getParameterCount() == 1 && methodName.startsWith("set")) {
                String propertySuffix = methodName.substring(3);
                final String name = StringUtils.uncapitalize(propertySuffix);

                if (exclusions.contains(name)) {
                    continue;
                }
                methodsByProperty
                        .computeIfAbsent(propertySuffix, k -> new ArrayList<>())
                        .add(method);
            }
        }

        for (Map.Entry<String, List<Method>> entry : methodsByProperty.entrySet()) {
            final String propertySuffix = entry.getKey();
            final String name = StringUtils.uncapitalize(propertySuffix);

            Method g = findGetter(target, propertySuffix);

            if (g == null) {
                continue;
            }

            Class<?> getterRawType = g.getReturnType();

            List<Method> candidateSetters = entry.getValue().stream()
                    .filter(m -> {
                        Class<?> paramType = m.getParameterTypes()[0];
                        return isSameType(paramType, getterRawType)
                                || getterRawType.isAssignableFrom(paramType)
                                || paramType.isAssignableFrom(getterRawType);
                    })
                    .collect(Collectors.toList());

            if (candidateSetters.isEmpty()) {
                candidateSetters = entry.getValue();
            }

            Method bestMethod = resolveBestSetter(candidateSetters, getterRawType);
            TypePair type = TypePair.ofParameter(bestMethod, 0);

            TypePair getterType = TypePair.ofReturnType(g);
            if (type.rawType.isAssignableFrom(getterType.rawType)) {
                type = getterType;
            }

            if (Map.class.isAssignableFrom(type.rawType)) {
                // yaml has support for Maps, but as nobody seem to like them we agreed not to support them
                LOGGER.log(Level.FINER, "{0} is a Map<?,?>. We decided not to support Maps.", name);
                continue;
            }

            final TypePair finalType = type;

            @SuppressWarnings("unchecked")
            Attribute<T, Object> rawAttribute = (Attribute<T, Object>) createAttribute(name, type);
            if (rawAttribute == null) {
                continue;
            }
            rawAttribute.setter((targetInstance, value) -> {
                Object finalValue = value;

                if (value instanceof Collection<?> collection) {
                    if (finalType.rawType.isArray()) {
                        Object array = newInstance(rawAttribute.getType(), collection.size());
                        int i = 0;
                        for (Object item : collection) {
                            set(array, i++, item);
                        }
                        finalValue = array;

                    } else if (SortedSet.class.isAssignableFrom(finalType.rawType)) {
                        finalValue = new TreeSet<>(collection);

                    } else if (Set.class.isAssignableFrom(finalType.rawType)) {
                        finalValue = new LinkedHashSet<>(collection);
                    }
                }

                bestMethod.invoke(targetInstance, finalValue);
            });

            rawAttribute.deprecated(bestMethod.getAnnotation(Deprecated.class) != null);
            final Restricted r = bestMethod.getAnnotation(Restricted.class);
            if (r != null) {
                rawAttribute.restrictions(r.value());
            }

            Attribute<T, ?> prevAttribute = attributes.get(name);
            if (prevAttribute == null || ((Class<?>) prevAttribute.type).isAssignableFrom(rawAttribute.type)) {
                attributes.put(name, rawAttribute);
            }
        }

        return new HashSet<>(attributes.values());
    }

    /**
     * Check if target class has a Getter method for property s
     */
    private Method findGetter(Class<T> c, String s) {
        String getMethod = "get" + s;
        String isMethod = "is" + s;

        for (Method m : c.getMethods()) {
            if (m.getParameterCount() == 0) {
                if (m.getName().equals(getMethod)) {
                    return m;
                }

                if (m.getName().equals(isMethod)) {
                    Class<?> returnType = m.getReturnType();
                    if (returnType == boolean.class || returnType == Boolean.class) {
                        return m;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Attribute names that are detected by introspection but should be excluded
     */
    protected Set<String> exclusions() {
        return Collections.emptySet();
    }

    protected Attribute createAttribute(String name, final TypePair type) {

        boolean multiple = type.rawType.isArray() || Collection.class.isAssignableFrom(type.rawType);

        // If attribute is a Collection|Array of T, we need to introspect further to determine T
        Class c = multiple ? getComponentType(type) : type.rawType;
        if (c == null) {
            throw new IllegalStateException("Unable to detect type of attribute " + getTarget() + '#' + name);
        }

        // special collection types with dedicated handlers to manage data replacement / possible values
        if (DescribableList.class.isAssignableFrom(type.rawType)) {
            return new DescribableListAttribute(name, c);
        } else if (PersistedList.class.isAssignableFrom(type.rawType)) {
            return new PersistedListAttribute(name, c);
        }

        Attribute attribute;
        if (!c.isPrimitive() && !c.isEnum() && Modifier.isAbstract(c.getModifiers())) {
            if (!Describable.class.isAssignableFrom(c)) {
                // Not a Describable, so we don't know how to detect concrete implementation type
                LOGGER.warning("Can't handle " + getTarget() + "#" + name + ": type is abstract but not Describable.");
                return null;
            }
            attribute = new DescribableAttribute(name, c);
        } else {
            attribute = new Attribute(name, c);
        }

        attribute.multiple(multiple);

        return attribute;
    }

    /**
     * Introspect the actual component type of a collection|array {@link Type}.
     */
    private Class getComponentType(TypePair type) {
        Class c = null;
        Type t = type.type;
        Class raw = type.rawType;

        // First, we need to introspect class hierarchy until we found a parameterized type.
        // for sample if type is Hudson.CloudList
        // Hudson.CloudList extends Jenkins.CloudList extends DescribableList<Cloud,Descriptor<Cloud>>
        // we need to get t = DescribableList<?,?> to actually retrieve component type
        while (t instanceof Class) {
            final Type superclass = ((Class) t).getGenericSuperclass();
            if (superclass == null) {
                // No parameterized type in class hierarchy
                t = raw;
                break;
            }
            t = superclass;
        }

        if (t instanceof GenericArrayType) {
            // t is a parameterized array: <Foo>[]
            GenericArrayType at = (GenericArrayType) t;
            t = at.getGenericComponentType();
        }
        while (t instanceof ParameterizedType) {
            // t is parameterized `Some<Foo>`
            ParameterizedType pt = (ParameterizedType) t;

            t = pt.getActualTypeArguments()[0];
            if (t instanceof WildcardType) {
                // pt is Some<? extends Foo>
                t = ((WildcardType) t).getUpperBounds()[0];
                if (t == Object.class) {
                    // pt is Some<?>, so we actually want "Some"
                    t = pt.getRawType();
                }
            }
        }

        while (c == null) {
            if (t instanceof Class) {
                c = (Class) t;
            } else if (t instanceof TypeVariable) {

                // t is declared as parameterized t
                // unfortunately, java reflection doesn't allow to get the actual parameter t
                // so, if superclass it parameterized, we assume parameter t match
                // i.e target is Foo extends AbstractFoo<Bar> with
                // public abstract class AbstractFoo<T> { void setBar(T bar) }
                final Type superclass = getTarget().getGenericSuperclass();
                if (superclass instanceof ParameterizedType) {
                    final ParameterizedType psc = (ParameterizedType) superclass;
                    t = psc.getActualTypeArguments()[0];
                } else {
                    c = (Class) ((TypeVariable) t).getBounds()[0];
                }
            } else {
                return null;
            }
        }

        if (c.isArray()) {
            c = c.getComponentType();
        }

        return c;
    }

    /**
     * Build or identify the target component this configurator has to handle based on the provided configuration node.
     * @param mapping configuration for target component. Implementation may consume some entries to create a fresh new instance.
     * @param context Fully configured Jenkins object used as the starting point for this configuration.
     * @return instance to be configured, but not yet fully configured, see {@link #configure(Mapping, Object, boolean, ConfigurationContext)}
     * @throws ConfiguratorException something went wrong...
     */
    protected abstract T instance(Mapping mapping, ConfigurationContext context) throws ConfiguratorException;

    @NonNull
    @Override
    public T configure(CNode c, ConfigurationContext context) throws ConfiguratorException {
        final Mapping mapping = (c != null ? c.asMapping() : Mapping.EMPTY);
        final T instance = instance(mapping, context);
        if (instance instanceof Saveable) {
            try (BulkChange bc = new BulkChange((Saveable) instance)) {
                configure(mapping, instance, false, context);
                bc.commit();
            } catch (IOException e) {
                throw new ConfiguratorException("Failed to save " + instance, e);
            }
        } else {
            configure(mapping, instance, false, context);
        }

        return instance;
    }

    @Override
    public T check(CNode c, ConfigurationContext context) throws ConfiguratorException {
        final Mapping mapping = (c != null ? c.asMapping() : Mapping.EMPTY);
        final T instance = instance(mapping, context);
        configure(mapping, instance, true, context);
        return instance;
    }

    /**
     * Run configuration process on the target instance
     * @param config configuration to apply. Can be partial if {@link #instance(Mapping, ConfigurationContext)} did already used some entries
     * @param instance target instance to configure
     * @param dryrun only check configuration is valid regarding target component. Don't actually apply changes to jenkins controller instance
     * @param context Fully configured Jenkins object used as the starting point for this configuration.
     * @throws ConfiguratorException something went wrong...
     */
    protected void configure(Mapping config, T instance, boolean dryrun, ConfigurationContext context)
            throws ConfiguratorException {
        final Set<Attribute<T, ?>> attributes = describe();
        List<Attribute<T, ?>> sortedAttributes =
                attributes.stream().sorted(Configurator.extensionOrdinalSort()).collect(Collectors.toList());
        for (Attribute<T, ?> attribute : sortedAttributes) {

            final String name = attribute.getName();
            CNode sub = removeIgnoreCase(config, name);
            if (sub == null) {
                for (String alias : attribute.aliases) {
                    sub = removeIgnoreCase(config, alias);
                    if (sub != null) {
                        context.warning(
                                sub, "'" + alias + "' is an obsolete attribute name, please use '" + name + "'");
                        break;
                    }
                }
            }

            if (sub != null) {

                if (attribute.isDeprecated()) {
                    context.warning(config, "'" + attribute.getName() + "' is deprecated");
                    if (context.getDeprecated() == ConfigurationContext.Deprecation.reject) {
                        throw new ConfiguratorException("'" + attribute.getName() + "' is deprecated");
                    }
                }

                for (Class<? extends AccessRestriction> r : attribute.getRestrictions()) {
                    if (r == None.class) {
                        continue;
                    }
                    if (r == Beta.class && context.getRestricted() == ConfigurationContext.Restriction.beta) {
                        continue;
                    }
                    context.warning(config, "'" + attribute.getName() + "' is restricted: " + r.getSimpleName());
                    if (context.getRestricted() == ConfigurationContext.Restriction.reject) {
                        throw new ConfiguratorException(
                                "'" + attribute.getName() + "' is restricted: " + r.getSimpleName());
                    }
                }

                final Class k = attribute.getType();
                final Configurator configurator = context.lookupOrFail(k);

                // Check for duplicate entries in Describables
                if (attribute instanceof DescribableAttribute && !attribute.isMultiple() && sub instanceof Mapping) {
                    Mapping mapping = sub.asMapping();
                    // Count the number of entries that might be conflicting configurations
                    int configurableEntries = 0;
                    for (String key : mapping.keySet()) {
                        CNode value = mapping.get(key);
                        if (value != null && !(value instanceof Scalar && (((Scalar) value).getValue() == null || ((Scalar) value).toString().isEmpty()))) {
                            configurableEntries++;
                        }
                    }
                    
                    if (configurableEntries > 1) {
                        String message = String.format("Multiple configurations found for single-valued Describable '%s'. Conflicting entries: %s. Only one can be used.",
                                attribute.getName(), String.join(", ", mapping.keySet()));
                        context.warning(sub, message);
                        if (context.getUnknown() == ConfigurationContext.Unknown.reject) {
                            throw new ConfiguratorException(this, message);
                        }
                        LOGGER.warning(message);
                    }
                }

                final Object valueToSet;
                try {
                    if (attribute.isMultiple()) {
                        List<Object> values = new ArrayList<>();
                        for (CNode o : sub.asSequence()) {
                            Object value = dryrun ? configurator.check(o, context) : configurator.configure(o, context);
                            values.add(value);
                        }
                        valueToSet = values;
                    } else {
                        valueToSet = dryrun ? configurator.check(sub, context) : configurator.configure(sub, context);
                    }

                    if (!dryrun) {
                        ((Attribute) attribute).setValue(instance, valueToSet);
                    }
                } catch (ConfiguratorException ex) {
                    if (ex instanceof UnknownAttributesException) {
                        throw ex;
                    }
                    String childMessage = ex.getErrorMessage();

                    String message = StringUtils.isNotBlank(childMessage)
                            ? "Failed to configure '" + attribute.getName() + "': " + childMessage
                            : "Failed to configure attribute '" + attribute.getName() + "'";

                    throw ConfiguratorException.from(sub, configurator, attribute.getName(), message, ex.getCause());
                } catch (Exception ex) {
                    throw ConfiguratorException.from(
                            sub,
                            this,
                            attribute.getName(),
                            "Failed to set attribute '" + attribute.getName() + "'",
                            ex);
                }
            }
        }

        handleUnknown(config, context);
    }

    protected final void handleUnknown(Mapping config, ConfigurationContext context) throws ConfiguratorException {
        if (!config.isEmpty()) {
            final String invalid = StringUtils.join(config.keySet(), ',');
            List<String> validAttributes =
                    getAttributes().stream().map(Attribute::getName).collect(Collectors.toList());
            String baseErrorMessage = "Invalid configuration elements for type: ";
            final String message = baseErrorMessage + getTarget() + " : " + invalid + ".\n"
                    + "Available attributes : "
                    + StringUtils.join(validAttributes, ", ");
            context.warning(config, message);
            switch (context.getUnknown()) {
                case reject:
                    throw new UnknownAttributesException(this, baseErrorMessage, message, invalid, validAttributes);

                case warn:
                    LOGGER.warning(message);
                    break;
                default: // All cases in the ENUM is covered
            }
        }
    }

    protected @NonNull Mapping compare(T instance, T reference, ConfigurationContext context) throws Exception {

        Mapping mapping = new Mapping();
        for (Attribute attribute : getAttributes()) {
            if (attribute.equals(instance, reference)) {
                continue;
            }
            mapping.put(attribute.getName(), attribute.describe(instance, context));
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

    public static final class TypePair {

        final Type type;

        /**
         * Erasure of {@link #type}
         */
        final Class<?> rawType;

        public TypePair(Type type, Class<?> rawType) {
            this.rawType = rawType;
            this.type = type;
        }

        static TypePair ofReturnType(Method method) {
            return new TypePair(method.getGenericReturnType(), method.getReturnType());
        }

        static TypePair ofParameter(Method method, int index) {
            assert method.getParameterCount() > index;
            return new TypePair(method.getGenericParameterTypes()[index], method.getParameterTypes()[index]);
        }

        public static TypePair of(Parameter parameter) {
            return new TypePair(parameter.getParameterizedType(), parameter.getType());
        }

        public static TypePair of(Field field) {
            return new TypePair(field.getGenericType(), field.getType());
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BaseConfigurator) {
            return getTarget() == ((BaseConfigurator) obj).getTarget();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getTarget().hashCode();
    }

    @Restricted(NoExternalUse.class)
    Method resolveBestSetter(List<Method> methods, Class<?> getterRawType) {
        List<Method> realMethods =
                methods.stream().filter(m -> !m.isBridge() && !m.isSynthetic()).collect(Collectors.toList());
        if (!realMethods.isEmpty()) {
            methods = realMethods;
        }
        if (methods.size() == 1) {
            return methods.get(0);
        }

        if (getterRawType != null) {
            for (Method m : methods) {
                if (isSameType(m.getParameterTypes()[0], getterRawType)) {
                    return m;
                }
            }
        }
        List<Method> arrayMethods =
                methods.stream().filter(m -> m.getParameterTypes()[0].isArray()).collect(Collectors.toList());
        if (!arrayMethods.isEmpty()) {
            methods = arrayMethods;
        }

        Method best = null;
        Class<?> bestType = null;

        for (Method m : methods) {
            Class<?> currentType = m.getParameterTypes()[0];

            if (best == null) {
                best = m;
                bestType = currentType;
                continue;
            }

            if (bestType.isAssignableFrom(currentType)) {
                best = m;
                bestType = currentType;
            } else if (!currentType.isAssignableFrom(bestType)) {
                boolean currentMatch = (getterRawType != null && getterRawType.isAssignableFrom(currentType));
                boolean bestMatch = (getterRawType != null && getterRawType.isAssignableFrom(bestType));

                if (currentMatch == bestMatch
                        && ((!currentType.isInterface() && bestType.isInterface())
                                || (currentType.isInterface() == bestType.isInterface()
                                        && currentType.getName().compareTo(bestType.getName()) < 0))) {

                    best = m;
                    bestType = currentType;
                }
            }
        }
        return best;
    }

    private boolean isSameType(Class<?> a, Class<?> b) {
        if (a == b) {
            return true;
        }
        if (a.isPrimitive() && !b.isPrimitive()) {
            return isWrapper(b, a);
        }
        if (b.isPrimitive() && !a.isPrimitive()) {
            return isWrapper(a, b);
        }
        return false;
    }

    private boolean isWrapper(Class<?> wrapper, Class<?> primitive) {
        if (primitive == int.class) {
            return wrapper == Integer.class;
        }
        if (primitive == boolean.class) {
            return wrapper == Boolean.class;
        }
        if (primitive == long.class) {
            return wrapper == Long.class;
        }
        if (primitive == double.class) {
            return wrapper == Double.class;
        }
        if (primitive == float.class) {
            return wrapper == Float.class;
        }
        if (primitive == byte.class) {
            return wrapper == Byte.class;
        }
        if (primitive == char.class) {
            return wrapper == Character.class;
        }
        if (primitive == short.class) {
            return wrapper == Short.class;
        }
        return false;
    }
}
