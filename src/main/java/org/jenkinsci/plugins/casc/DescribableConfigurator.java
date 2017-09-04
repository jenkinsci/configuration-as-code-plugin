package org.jenkinsci.plugins.casc;

import com.google.common.base.Defaults;
import hudson.DescriptorExtensionList;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.ClassDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A generic {@link Configurator} to configure {@link Describable} which offer a
 * {@link org.kohsuke.stapler.DataBoundConstructor}
 *
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class DescribableConfigurator extends BaseConfigurator<Describable> {

    private final Class target;

    public DescribableConfigurator(Class clazz) {
        this.target = clazz;
    }

    @Override
    public Class<Describable> getTarget() {
        return target;
    }

    @Override
    public Describable configure(Object c) throws Exception {

        Map config = c instanceof Map ? (Map) c : Collections.EMPTY_MAP;

        final Constructor constructor = getDataBoundConstructor(target);
        if (constructor == null) {
            throw new IllegalStateException(target.getName() + " is missing a @DataBoundConstructor");
        }

        final Parameter[] parameters = constructor.getParameters();
        final String[] names = ClassDescriptor.loadParameterNames(constructor);
        Object[] args = new Object[names.length];

        if (parameters.length > 0) {
            // Many jenkins components haven't been migrated to @DataBoundSetter vs @NotNull constructor parameters
            // as a result it might be valid to reference a describable without parameters

            for (int i = 0; i < names.length; i++) {
                final Object value = config.remove(names[i]);
                if (value == null && parameters[i].getAnnotation(Nonnull.class) != null) {
                    throw new IllegalArgumentException(names[i] + " is required to configure " + target);
                }
                final Class t = parameters[i].getType();
                if (value != null) {
                    if (Collection.class.isAssignableFrom(t)) {
                        if (!(value instanceof List)) {
                            throw new IllegalArgumentException(names[i] + " should be a list");
                        }
                        final Type pt = parameters[i].getParameterizedType();
                        final Configurator lookup = Configurator.lookup(pt);

                        final ArrayList<Object> list = new ArrayList<>();
                        for (Object o : (List) value) {
                            list.add(lookup.configure(o));
                        }
                        args[i] = list;

                    } else {
                        final Type pt = parameters[i].getParameterizedType();
                        args[i] = Configurator.lookup(pt != null ? pt : t).configure(value);
                    }
                    System.out.println("Setting " + target + "." + names[i] + " = " + value);
                } else if (t.isPrimitive()) {
                    args[i] = Defaults.defaultValue(t);
                }
            }
        }
        Describable object = (Describable) constructor.newInstance(args);

        final Set<Attribute> attributes = describe();

        for (Attribute attribute : attributes) {
            final String name = attribute.getName();
            if (config.containsKey(name)) {
                final Object value = Configurator.lookup(attribute.getType()).configure(config.get(name));
                attribute.setValue(object, value);
            }
        }
        return object;
    }

    public Constructor getDataBoundConstructor(Class type) {
        for (Constructor c : type.getConstructors()) {
            if (c.getAnnotation(DataBoundConstructor.class) != null) return c;
        }
        return null;

    }


    public String getName() {
        final Descriptor d = getDescriptor();
        final Symbol annotation = d.getClass().getAnnotation(Symbol.class);
        if (annotation != null) return annotation.value()[0];
        return getTarget().getSimpleName();
    }

    private Descriptor getDescriptor() {
        return Jenkins.getInstance().getDescriptor(getTarget());
    }

    public Class getExtensionpoint() {

        // detect common pattern DescriptorImpl extends Descriptor<ExtensionPoint>
        final Type superclass = getDescriptor().getClass().getGenericSuperclass();
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
        return super.getExtensionpoint();
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
                final Attribute a = new Attribute(names[i], p.getType());
                attributes.add(a);
            }
        }

        return attributes;
    }


    public String getDisplayName() {
        final List<Descriptor> list = Jenkins.getInstance().getDescriptorList(target);
        return list.get(0).getDisplayName();
    }
}
