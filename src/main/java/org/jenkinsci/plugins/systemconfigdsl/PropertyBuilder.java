package org.jenkinsci.plugins.systemconfigdsl;

import groovy.lang.Closure;
import org.kohsuke.stapler.DataBoundSetter;

import java.beans.Introspector;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Builds up values into {@link #properties} either to be set to an existing object
 * or to be assigned to the newly instantiated object.
 *
 * @author Kohsuke Kawaguchi
 */
/*package*/ class PropertyBuilder extends BuilderSupport {
    public final Class type;
    public final Map<String,Property> properties = new HashMap<String, Property>();

    public PropertyBuilder(Class type) {
        this.type = type;

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

    /**
     * "x = 5" works as accumulation of a property
     */
    @Override
    public void setProperty(String property, Object value) {
        Property p = properties.get(property);
        if (p==null)
            throw new IllegalArgumentException("Property '"+property+"' does not exist on "+type);
        p.add(value);
    }

    @Override
    public Object invokeMethod(String name, Object _args) {
        Object[] args = massageArgs(_args);

        Property p = properties.get(name);
        if (p==null)
            throw new IllegalArgumentException("Property '"+name+"' does not exist on "+type);

        if (p.collection && matches(args,Closure.class)) {
            Closure closure = (Closure) args[0];

            // collection building closure
            closure.setDelegate(new CollectionBuilder(p));
            closure.setResolveStrategy(Closure.DELEGATE_FIRST);
            return closure.call();
        }
        if (matches(args,String.class,Closure.class)) {
            // value building closure
            Object v = buildValue((String) args[0], (Closure) args[1]);
            if (v!=null) {
                p.add(v);
                return null;
            }
        }
        if (matches(args,String.class) && p.type!=String.class) {
            // value building without closure, if the type of the property is not String
            Object v = buildValue((String) args[0], null);
            if (v!=null) {
                p.add(v);
                return null;
            }
        }
        if (matches(args,Object.class)) {
            // value assignment
            // prop VALUE
            p.add(args[0]);
            return null;
        }

        return super.invokeMethod(name, _args);
    }

    /**
     * Assigns all the accumulated property values into the target object.
     */
    protected void handleSetters(Object o) {
        for (Property p : properties.values())
            p.assignTo(o);
    }
}

