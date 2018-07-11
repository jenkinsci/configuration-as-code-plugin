package org.jenkinsci.plugins.casc;

import hudson.model.Describable;
import hudson.util.PersistedList;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.casc.impl.attributes.DescribableAttribute;
import org.jenkinsci.plugins.casc.impl.attributes.PersistedListAttribute;
import org.jenkinsci.plugins.casc.model.CNode;
import org.jenkinsci.plugins.casc.model.Mapping;
import org.kohsuke.accmod.AccessRestriction;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.Beta;
import org.kohsuke.accmod.restrictions.None;

import javax.annotation.Nonnull;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * a General purpose abstract {@link Configurator} implementation.
 * Target component is identified by implementing {@link #instance(Mapping)} then configuration is applied on
 * {@link Attribute}s as defined by {@link #describe()}.
 * This base implementation uses JavaBean convention to identify configurable attributes.
 *
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Restricted(Beta.class)
public abstract class BaseConfigurator<T> extends Configurator<T> {

    private static final Logger logger = Logger.getLogger(BaseConfigurator.class.getName());

    public Set<Attribute> describe() {

        Set<Attribute> attributes = new HashSet<>();

        for (Field field : getTarget().getFields()) {
            if (Modifier.isFinal(field.getModifiers())) {
                if (PersistedList.class.isAssignableFrom(field.getType())) {
                    // see Jenkins#clouds
                    Attribute attribute = detectActualType(field.getName(), TypePair.of(field));
                    attributes.add(attribute);
                }
            }
        }

        final Class<T> target = getTarget();
        for (Method method : target.getMethods()) {

            final String methodName = method.getName();
            TypePair type;
            if (method.getParameterCount() == 0 && methodName.startsWith("get")
                && PersistedList.class.isAssignableFrom(method.getReturnType())) {

                type = TypePair.ofReturnType(method);
            } else if (method.getParameterCount() != 1 || !methodName.startsWith("set")) {
                // Not an accessor, ignore
                continue;
            } else {
                type = TypePair.ofParameter(method, 0);
            }

            final String name = StringUtils.uncapitalize(methodName.substring(3));
            logger.log(Level.FINER, "Processing {0} property", name);

            Attribute attribute = detectActualType(name, type);
            if (attribute == null) continue;
            attributes.add(attribute);

            attribute.deprecated(method.getAnnotation(Deprecated.class) != null);
            final Restricted r = method.getAnnotation(Restricted.class);
            if (r != null) attribute.restrictions(r.value());
        }

        return attributes;
    }

    protected Attribute detectActualType(String name, final TypePair type) {
        Class c = introspectActualClass(type);
        if (c == null) {
            throw new IllegalStateException("Unable to detect type of attribute " + getTarget() + '#' + name);
        }

        Attribute attribute;
        if (PersistedList.class.isAssignableFrom(type.rawType)) {
            return new PersistedListAttribute(name, c);
        } else if (!c.isPrimitive() && !c.isEnum() && Modifier.isAbstract(c.getModifiers())) {
            if (!Describable.class.isAssignableFrom(c)) {
                // Not a Describable, so we don't know how to detect concrete implementation type
                return null;
            }
            attribute = new DescribableAttribute(name, c);
        } else {
            attribute = new Attribute(name, c);
        }

        boolean multiple =
                type.rawType.isArray()
            ||  Collection.class.isAssignableFrom(type.rawType);

        attribute.multiple(multiple);

        return attribute;
    }

    private Class introspectActualClass(TypePair type) {
        Class c = null;
        Type t = type.type;
        Class raw = type.rawType;

        // for Hudson.CloudList extends Jenkins.CloudList extends DescribableList<Cloud,Descriptor<Cloud>>
        // we need to check if generic superclass is owning the parameter infos
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
                // i.e target is Foo extends AbtractFoo<Bar> with
                // public abstract class AbtractFoo<T> { void setBar(T bar) }
                final Type superclass = getTarget().getGenericSuperclass();
                if (superclass instanceof ParameterizedType) {
                    final ParameterizedType psc = (ParameterizedType) superclass;
                    t = psc.getActualTypeArguments()[0];
                    continue;
                } else {
                    c = (Class) ((TypeVariable) t).getBounds()[0];
                }
                TypeVariable tv = (TypeVariable) t;
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
     * @return instance to be configured, but not yet fully configured, see {@link #configure(Mapping, Object, boolean)}
     * @throws ConfiguratorException
     */
    protected abstract T instance(Mapping mapping) throws ConfiguratorException;

    @Nonnull
    @Override
    public T configure(CNode c) throws ConfiguratorException {
        final Mapping mapping = (c != null ? c.asMapping() : Mapping.EMPTY);
        final T instance = instance(mapping);
        configure(mapping, instance, false);
        return instance;
    }


    @Override
    public T check(CNode c) throws ConfiguratorException {
        final Mapping mapping = (c != null ? c.asMapping() : Mapping.EMPTY);
        final T instance = instance(mapping);
        configure(mapping, instance, true);
        return instance;
    }

    /**
     * Run configuration process on the target instance
     * @param config configuration to apply. Can be partial if {@link #instance(Mapping)} did already used some entries
     * @param instance target instance to configure
     * @param dryrun only check configuration is valid regarding target component. Don't actually apply changes to jenkins master instance
     * @throws ConfiguratorException something went wrong...
     */
    protected final void configure(Mapping config, T instance, boolean dryrun) throws ConfiguratorException {
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

                if (!dryrun) {
                    try {
                        logger.info("Setting " + instance + '.' + name + " = " + (sub.isSensitiveData() ? "****" : valueToSet));
                        attribute.setValue(instance, valueToSet);
                    } catch (Exception ex) {
                        throw new ConfiguratorException(configurator, "Failed to set attribute " + attribute, ex);
                    }
                }
            }
        }

        handleUnknown(config);
    }

    protected final void handleUnknown(Mapping config) throws ConfiguratorException {
        if (!config.isEmpty()) {
            final String invalid = StringUtils.join(config.keySet(), ',');
            final String message = "Invalid configuration elements for type " + getTarget() + " : " + invalid;
            ObsoleteConfigurationMonitor.get().record(config, message);
            switch (ConfigurationAsCode.get().getUnknown()) {
                case reject:
                    throw new ConfiguratorException(message);

                case warn:
                    logger.warning(message);
            }
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

    @Restricted(Beta.class)
    public static final class TypePair {

        final Type type;

        /**
         * Erasure of {@link #type}
         */
        final Class rawType;

        public TypePair(Type type, Class rawType) {
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
            return new TypePair(parameter.getParameterizedType(), parameter.getType());        }

        public static TypePair of(Field field) {
            return new TypePair(field.getGenericType(), field.getType());
        }
    }

}
