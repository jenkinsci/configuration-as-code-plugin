package org.jenkinsci.plugins.systemconfigdsl;

import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;
import groovy.lang.MetaClass;
import hudson.model.Describable;
import jenkins.model.Jenkins;
import org.codehaus.groovy.runtime.InvokerHelper;

/**
 * @author Kohsuke Kawaguchi
 */
public class Surrogate extends GroovyObjectSupport {
    private MetaClass metaClass;
    private final Object target;

    public Surrogate(Object target) {
        this.target = target;
    }

    @Override
    public MetaClass getMetaClass() {
        if (metaClass == null) {
            metaClass = InvokerHelper.getMetaClass(target.getClass());
        }
        return metaClass;
    }

    /**
     * Pass through set property call to the target object.
     */
    @Override
    public void setProperty(String property, Object newValue) {
        getMetaClass().setProperty(target, property, newValue);
    }

    // this probably isn't needed but let's pass through getProperty too
    @Override
    public Object getProperty(String property) {
        return getMetaClass().getProperty(target, property);
    }

    @Override
    public Object invokeMethod(String name, Object args) {
        Object arg = unpackSoleArg(args);
        if (arg instanceof Closure) {
            Closure closure = (Closure) arg;
            Class describable = map(name);
            if (describable!=null) {
                DescribableFactory f = new DescribableFactory(describable);
                closure.setDelegate(f);
                closure.call();
                return f.instantiate();
            }
        }
        return super.invokeMethod(name, args);
    }

    private Object unpackSoleArg(Object args) {
        if (args instanceof Object[])
            return ((Object[])args)[0];
        else
            return args;
    }

    private Class map(String name) {
        // TODO
        try {
            Class<?> c = Jenkins.getInstance().pluginManager.uberClassLoader.loadClass(name.replace('_', '.'));
            return c.asSubclass(Describable.class);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}
