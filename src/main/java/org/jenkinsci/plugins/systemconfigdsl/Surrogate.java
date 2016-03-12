package org.jenkinsci.plugins.systemconfigdsl;

import groovy.lang.MetaClass;
import org.codehaus.groovy.runtime.InvokerHelper;

import java.beans.Introspector;
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

        for (Method m : type.getMethods()) {
            if (m.getName().startsWith("set") && m.getParameterTypes().length==1) {
                String n = Introspector.decapitalize(m.getName().substring(3));
                properties.put(n,new Property(n,m.getGenericParameterTypes()[0],Setter.create(m)));
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
