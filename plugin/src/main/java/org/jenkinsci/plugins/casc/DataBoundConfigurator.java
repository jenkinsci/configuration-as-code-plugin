package org.jenkinsci.plugins.casc;

import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.casc.model.CNode;
import org.jenkinsci.plugins.casc.model.Mapping;
import org.kohsuke.stapler.ClassDescriptor;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.common.base.Defaults.defaultValue;

/**
 * A generic {@link Configurator} to configure components which offer a
 * {@link org.kohsuke.stapler.DataBoundConstructor}
 *
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class DataBoundConfigurator<T> extends BaseConfigurator<T> {

    private final static Logger logger = Logger.getLogger(DataBoundConfigurator.class.getName());

    private final Class<T> target;

    public DataBoundConfigurator(Class<T> clazz) {
        this.target = clazz;
    }

    @Override
    public Class getTarget() {
        return target;
    }

    @Override
    public T configure(CNode c) throws ConfiguratorException {

        // c can be null for component with no-arg constructor and no extra property to be set
        Mapping config = (c != null ? c.asMapping() : Mapping.EMPTY);

        // TODO: The fallback resolution may end up resolving empty constructors
        // Ideally we need an annotation or whatever other logic that old constructors are acceptable
        final Constructor dataBoundConstructor = getDataBoundConstructor();
        T object = null;
        try {
            logger.log(Level.INFO, "Trying @DataBoundConstructor for target {0}: {1}",
                    new Object[] {target, dataBoundConstructor} );
            object = tryConstructor((Constructor<T>) dataBoundConstructor, config);
        } catch (ConfiguratorException ex) {
            logger.log(Level.INFO, "Default databound constructor cannot be applied, " +
                    "will consult with Legacy DataBoundConstructor providers", ex);
            for (Constructor constructor : LegacyDataBoundConstructorProvider.getLegacyDataBoundConstructors(target)) {
                if (constructor == dataBoundConstructor) {
                    continue; // Already tried it
                }
                logger.log(Level.INFO, "Trying legacy constructor {0} for target {1}",
                        new Object[] {constructor, target});

                try {
                    object = tryConstructor((Constructor<T>) constructor, config);
                } catch (ConfiguratorException ex2) {
                    logger.log(Level.INFO, "Constructor {0} didn't work for target {1}",
                            new Object[] {constructor, target});
                }

                if (object != null) {
                    break;
                }
            }
        }
        if (object == null) {
            throw new ConfiguratorException("Failed to find a compatible constructor for target " + target);
        }

        final Set<Attribute> attributes = describe();

        for (Attribute attribute : attributes) {
            final String name = attribute.getName();
            final Configurator lookup = Configurator.lookup(attribute.getType());
            if (config.containsKey(name)) {
                final CNode yaml = config.get(name);
                Object value;
                if (attribute.isMultiple()) {
                    List l = new ArrayList<>();
                    for (CNode o : yaml.asSequence()) {
                        l.add(lookup.configure(o));
                    }
                    value = l;
                } else {
                    value = lookup.configure(config.get(name));
                }
                try {
                    logger.info("Setting " + object + '.' + name + " = " + (yaml.isSensitiveData() ? "****" : value));
                    attribute.setValue(object, value);
                } catch (Exception e) {
                    throw new ConfiguratorException(this, "Failed to set attribute " + attribute, e);
                }
            }
        }

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

    private T tryConstructor(Constructor<T> constructor, Mapping config) throws ConfiguratorException {
        final Parameter[] parameters = constructor.getParameters();
        final String[] names = ClassDescriptor.loadParameterNames(constructor);
        Object[] args = new Object[names.length];

        if (parameters.length > 0) {
            // Many jenkins components haven't been migrated to @DataBoundSetter vs @NotNull constructor parameters
            // as a result it might be valid to reference a describable without parameters

            for (int i = 0; i < names.length; i++) {
                final CNode value = config.remove(names[i]);
                if (value == null && parameters[i].getAnnotation(Nonnull.class) != null) {
                    throw new ConfiguratorException(names[i] + " is required to configure " + target);
                }
                final Class t = parameters[i].getType();
                if (value != null) {
                    if (Collection.class.isAssignableFrom(t)) {
                        final Type pt = parameters[i].getParameterizedType();
                        final Configurator lookup = Configurator.lookup(pt);

                        final ArrayList<Object> list = new ArrayList<>();
                        for (CNode o : value.asSequence()) {
                            list.add(lookup.configure(o));
                        }
                        args[i] = list;

                    } else {
                        final Type pt = parameters[i].getParameterizedType();
                        final Type k = pt != null ? pt : t;
                        final Configurator configurator = Configurator.lookup(k);
                        if (configurator == null) throw new ConfiguratorException("No configurator implementation to manage "+k);
                        args[i] = configurator.configure(value);
                    }
                    logger.info("Setting " + target + "." + names[i] + " = " + (value.isSensitiveData() ? "****" : value));
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
            throw new ConfiguratorException(this,
                    "Failed to construct instance of " + target +
                            ".\n Constructor: " + constructor.toString() +
                            ".\n Arguments: " + argumentTypes, ex);
        }

        return object;
    }

    public String getName() {
        final Descriptor d = getDescriptor();
        return DescribableAttribute.getPreferredSymbol(d, getExtensionPoint(), getTarget());
    }

    private Descriptor getDescriptor() {
        return Jenkins.getInstance().getDescriptor(getTarget());
    }

    public Class getExtensionPoint() {

        final Descriptor descriptor = getDescriptor();
        if (descriptor != null) {
            // detect common pattern DescriptorImpl extends Descriptor<ExtensionPoint>
            final Type superclass = descriptor.getClass().getGenericSuperclass();
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
        return super.getExtensionPoint();
    }


    @Override
    public Set<Attribute> describe() {
        final Set<Attribute> attributes = super.describe();

        final Constructor constructor = getDataBoundConstructor(target);

        if (constructor != null) {
            final Parameter[] parameters = constructor.getParameters();
            final String[] names = ClassDescriptor.loadParameterNames(constructor);
            for (int i = 0; i < parameters.length; i++) {
                final Parameter p = parameters[i];
                final Attribute a = detectActualType(names[i], TypePair.of(p));
                if (a == null) continue;
                attributes.add(a);
            }
        }

        return attributes;
    }

    @CheckForNull
    @Override
    public CNode describe(T instance) throws Exception {

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
            final Attribute a = detectActualType(names[i], TypePair.of(p));
            args[i] = a.getValue(instance);
            if (args[i] == null && p.getType().isPrimitive()) {
                args[i] = defaultValue(p.getType());
            }
            attributes[i] = a;
        }

        T ref = (T) constructor.newInstance(args);

        // compare instance with this "default" object
        Mapping mapping = compare(instance, ref);

        // add constructor parameters
        for (int i = 0; i < parameters.length; i++) {
            final Configurator c = Configurator.lookup(attributes[i].getType());
            if (args[i] == null) continue;
            mapping.put(names[i], attributes[i].describe(instance));
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
        return descriptor != null ? descriptor.getDisplayName() : "";
    }
}
