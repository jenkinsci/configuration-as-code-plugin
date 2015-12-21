package org.jenkinsci.plugins.systemconfigdsl;

import groovy.lang.Closure;
import hudson.model.Describable;
import hudson.model.Descriptor;

/**
 * @author Kohsuke Kawaguchi
 */
public class CollectionBuilder extends BuilderSupport {
    private final Property prop;
    private final Class<? extends Descriptor> descriptorType;

    public CollectionBuilder(Property prop) {
        this.prop = prop;
        if (!Describable.class.isAssignableFrom(prop.itemType))
            throw new IllegalStateException(prop.itemType+" is not Describable");
        try {
            descriptorType = (Class)prop.itemType.getMethod("getDescriptor").getReturnType();
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(prop.itemType+" does not have the getDescriptor method");
        }
    }

    @Override
    public Object invokeMethod(String name, Object _args) {
        Object[] args = wrap(_args);

        if (matches(args,Closure.class)) {
            // value building closure
            Object v = buildValue(name, (Closure) args[0]);
            if (v!=null) {
                prop.add(v);
                return v;
            }
        }

        return super.invokeMethod(name, args);
    }
}
