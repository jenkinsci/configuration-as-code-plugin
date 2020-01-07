package io.jenkins.plugins.casc.impl.configurators;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Descriptor;
import hudson.util.Secret;
import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.BaseConfigurator;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.Configurator;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.impl.attributes.DescribableAttribute;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.PostConstruct;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.ClassDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.Stapler;

import static com.google.common.base.Defaults.defaultValue;

/**
 * A generic {@link Configurator} to configure components with a {@link org.kohsuke.stapler.DataBoundConstructor}.
 * Intended to replicate Stapler's request-to-instance lifecycle, including {@link PostConstruct} init methods.
 * Will rely on <a href="https://github.com/jenkinsci/jep/tree/master/jep/205">JEP-205</a> once implemented
 *
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */

public class DataBoundConfigurator<T> extends BaseConfigurator<T> {

    private static final Logger LOGGER = Logger.getLogger(DataBoundConfigurator.class.getName());

    private final Class<T> target;

    public DataBoundConfigurator(Class<T> clazz) {
        this.target = clazz;
    }

    @CheckForNull
    public static Constructor getDataBoundConstructor(@NonNull Class type) {
        for (Constructor c : type.getConstructors()) {
            if (c.getAnnotation(DataBoundConstructor.class) != null) return c;
        }
        return null;

    }

    @Override
    public Class getTarget() {
        return target;
    }

    /**
     * Build a fresh new component based on provided configuration and {@link org.kohsuke.stapler.DataBoundConstructor}
     */
    @Override
    protected T instance(Mapping config, ConfigurationContext context) throws ConfiguratorException {
        final Constructor dataBoundConstructor = getDataBoundConstructor();
        return tryConstructor((Constructor<T>) dataBoundConstructor, config, context);
    }

    @NonNull
    @Override
    public T configure(CNode c, ConfigurationContext context) throws ConfiguratorException {
        T object = super.configure(c, context);

        for (Method method : target.getMethods()) {
            if (method.getParameterCount() == 0 && method.getAnnotation(PostConstruct.class) != null) {
                try {
                    method.invoke(object, null);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new ConfiguratorException(this, "Failed to invoke configurator method " + method, e);
                }
            }
        }
        return object;
    }

    @Override
    public T check(CNode config, ConfigurationContext context) throws ConfiguratorException {
        // As DataBound objects are replaced in jenkins model we can build one from configuration without side-effects
        // BUT we don't invoke @PostConstruct methods which might un some post-build registration into jenkins APIs.
        return super.configure(config, context);
    }

    private T tryConstructor(Constructor<T> constructor, Mapping config, ConfigurationContext context) throws ConfiguratorException {
        final Parameter[] parameters = constructor.getParameters();
        final String[] names = ClassDescriptor.loadParameterNames(constructor);
        Object[] args = new Object[names.length];

        if (parameters.length > 0) {
            // Many jenkins components haven't been migrated to @DataBoundSetter vs @NotNull constructor parameters
            // as a result it might be valid to reference a describable without parameters

            for (int i = 0; i < names.length; i++) {
                final CNode value = config.get(names[i]);
                final Class t = parameters[i].getType();

                Class<?> clazz = constructor.getDeclaringClass();
                if (value == null && (parameters[i].isAnnotationPresent(Nonnull.class)   ||
                    constructor.isAnnotationPresent(ParametersAreNonnullByDefault.class) ||
                    clazz.isAnnotationPresent(ParametersAreNonnullByDefault.class)       ||
                    clazz.getPackage().isAnnotationPresent(ParametersAreNonnullByDefault.class) &&
                        !parameters[i].isAnnotationPresent(CheckForNull.class))) {

                    if (Set.class.isAssignableFrom(t)) {
                        LOGGER.log(Level.FINER, "The parameter to be set is @Nonnull but is not present; " +
                                                           "setting equal to empty set.");
                        args[i] = Collections.emptySet();
                    } else if (List.class.isAssignableFrom(t)) {
                        LOGGER.log(Level.FINER, "The parameter to be set is @Nonnull but is not present; " +
                                                           "setting equal to empty list.");
                        args[i] = Collections.emptyList();
                    } else {
                        throw new ConfiguratorException(names[i] + " is required to configure " + target);
                    }
                    continue;
                }

                if (value != null) {
                    if (Collection.class.isAssignableFrom(t)) {
                        final Type pt = parameters[i].getParameterizedType();
                        final Configurator lookup = context.lookupOrFail(pt);

                        final Collection<Object> collection;

                        if (Set.class.isAssignableFrom(t)) {
                            collection = new HashSet<>();
                        } else {
                            collection = new ArrayList<>();
                        }

                        for (CNode o : value.asSequence()) {
                            collection.add(lookup.configure(o, context));
                        }
                        args[i] = collection;

                    } else {
                        final Type pt = parameters[i].getParameterizedType();
                        final Type k = pt != null ? pt : t;
                        final Configurator configurator = context.lookupOrFail(k);
                        args[i] = configurator.configure(value, context);
                    }
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.log(Level.FINE, "Setting {0}.{1} = {2}",
                                new Object[]{target, names[i], t == Secret.class || Attribute.calculateIfSecret(target, names[i]) ? "****" : value});
                    }
                } else if (t.isPrimitive()) {
                    args[i] = defaultValue(t);
                }
            }
        }

        final T object;
        try {
            object = constructor.newInstance(args);
        } catch (IllegalArgumentException | InstantiationException | InvocationTargetException | IllegalAccessException ex) {
            List<String> argumentTypes = new ArrayList<>(args.length);
            for (Object arg : args) {
                argumentTypes.add(arg != null ? arg.getClass().getName() : "null");
            }
            List<String> parameterTypes = new ArrayList<>(parameters.length);
            for (Parameter parameter: parameters){
                parameterTypes.add(parameter.getParameterizedType().getTypeName());
            }
            List<String> expectedParamList = new ArrayList<>(parameters.length);
            for(int i = 0; i < parameters.length; i++){
                expectedParamList.add(names[i] + " " + parameterTypes.get(i));
            }
            throw new ConfiguratorException(this,
                    "Failed to construct instance of " + target +
                            ".\n Constructor: " + constructor.toString() +
                            ".\n Arguments: " + argumentTypes +
                            ".\n Expected Parameters: " + String.join(", ",expectedParamList)
                    , ex);
        }

        // constructor was successful, so let's removed configuration elements we have consumed doing so.
        for (String name : names) {
            config.remove(name);
        }

        return object;
    }

    @NonNull
    public String getName() {
        final Descriptor d = getDescriptor();
        return DescribableAttribute.getPreferredSymbol(d, getImplementedAPI(), getTarget());
    }

    private Descriptor getDescriptor() {
        return Jenkins.getInstance().getDescriptor(getTarget());
    }

    @NonNull
    public Class getImplementedAPI() {

        final Descriptor descriptor = getDescriptor();
        if (descriptor != null) {
            // traverse Descriptor's class hierarchy until we found "extends Descriptor<ExtensionPoint>"
            Class c = descriptor.getClass();
            Type superclass;
            do {
                superclass = c.getGenericSuperclass();
                c = c.getSuperclass();
            } while (c != Descriptor.class);


            if (superclass instanceof ParameterizedType) {
                final ParameterizedType genericSuperclass = (ParameterizedType) superclass;
                Type type = genericSuperclass.getActualTypeArguments()[0];
                if (type instanceof ParameterizedType) {
                    type = ((ParameterizedType) type).getRawType();
                }
                if (type instanceof Class) {
                    return (Class) type;
                }
            }
        }
        return super.getImplementedAPI();
    }


    @NonNull
    @Override
    public Set<Attribute<T,?>> describe() {
        final Set<Attribute<T,?>> attributes = super.describe();

        final Constructor constructor = getDataBoundConstructor(target);

        if (constructor != null) {
            final Parameter[] parameters = constructor.getParameters();
            final String[] names = ClassDescriptor.loadParameterNames(constructor);
            for (int i = 0; i < parameters.length; i++) {
                final Parameter p = parameters[i];
                final Attribute a = createAttribute(names[i], TypePair.of(p));
                if (a == null) continue;
                attributes.add(a);
            }
        }

        return attributes;
    }

    @CheckForNull
    @Override
    public CNode describe(T instance, ConfigurationContext context) throws Exception {

        // Here we assume a correctly designed DataBound Object will have required attributes set by DataBoundConstructor
        // and all others using DataBoundSetters. So constructor parameters for sure are part of the description, others
        // need to be compared with default values.

        // Build same object with only constructor parameters
        final Constructor constructor = getDataBoundConstructor();

        final Parameter[] parameters = constructor.getParameters();
        final String[] names = ClassDescriptor.loadParameterNames(constructor);
        final Attribute[] attributes = new Attribute[parameters.length];
        final Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            final Parameter p = parameters[i];
            final Attribute a = createAttribute(names[i], TypePair.of(p));
            Object value = a.getValue(instance);
            if (value != null) {
                Object converted = Stapler.CONVERT_UTILS.convert(value, a.getType());
                if (converted instanceof Collection || p.getType().isArray() || !a.isMultiple()) {
                    args[i] = converted;
                } else if (Set.class.isAssignableFrom(p.getType())) {
                    args[i] = Collections.singleton(converted);
                } else {
                    args[i] = Collections.singletonList(converted);
                }
            }
            if (args[i] == null && p.getType().isPrimitive()) {
                args[i] = defaultValue(p.getType());
            }
            attributes[i] = a;
        }

        T ref = (T) constructor.newInstance(args);

        // compare instance with this "default" object
        Mapping mapping = compare(instance, ref, context);

        // add constructor parameters
        for (int i = 0; i < parameters.length; i++) {
            if (args[i] == null) continue;
            mapping.put(names[i], attributes[i].describe(instance, context));
        }

        return mapping;
    }

    /**
     * Gets DataBoundConstructor or fails.
     * @return constructor with {@link org.kohsuke.stapler.DataBoundConstructor} annotation
     * @throws ConfiguratorException Constructor not found
     */
    private Constructor getDataBoundConstructor() throws ConfiguratorException {
        final Constructor constructor = getDataBoundConstructor(target);
        if (constructor == null) {
            throw new ConfiguratorException(target.getName() + " is missing a @DataBoundConstructor");
        }
        return constructor;
    }

    public String getDisplayName() {
        final Descriptor descriptor = getDescriptor();
        return descriptor != null ? descriptor.getDisplayName() : getName();
    }
}
