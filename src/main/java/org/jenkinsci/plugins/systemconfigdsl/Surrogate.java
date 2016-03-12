package org.jenkinsci.plugins.systemconfigdsl;

import groovy.lang.MetaClass;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.kohsuke.stapler.DataBoundSetter;

import java.beans.Introspector;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Closure delegate that configures an existing instance.
 *
 * <p>
 * After evaluating a closure, call {@link #assign()} to
 * actually set values to the target object.
 *
 * @author Kohsuke Kawaguchi
 */
public class Surrogate extends PropertyBuilder {
    private MetaClass metaClass;
    private final Object target;

    public Surrogate(Object target) {
        super(target.getClass());
        this.target = target;

        for (Class c=type; c!=null; c=c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getName().startsWith("set") && m.getParameterTypes().length==1) {
//                if (m.isAnnotationPresent(DataBoundSetter.class)) {
                    String n = Introspector.decapitalize(m.getName().substring(3));
                    properties.put(n,new Property(n,m.getGenericParameterTypes()[0],Setter.create(m)));
                }
            }
            for (Field f : c.getDeclaredFields()) {
                if (f.isAnnotationPresent(DataBoundSetter.class)) {
                    String n = f.getName();
                    properties.put(n,new Property(n,f.getGenericType(),Setter.create(f)));
                }
            }
        }
    }

    @Override
    public MetaClass getMetaClass() {
        if (metaClass == null) {
            metaClass = InvokerHelper.getMetaClass(target.getClass());
        }
        return metaClass;
    }

    /**
     * Set all the accumulated properties into the target object.
     */
    public void assign() {
        handleSetters(target);
    }
}
