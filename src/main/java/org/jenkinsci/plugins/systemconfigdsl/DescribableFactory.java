package org.jenkinsci.plugins.systemconfigdsl;

import groovy.lang.GroovyObjectSupport;
import org.kohsuke.stapler.ClassDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.NoStaplerConstructorException;

import java.beans.Introspector;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Kohsuke Kawaguchi
 */
public class DescribableFactory extends GroovyObjectSupport {
    private final Class type;
    private final Map<String,Property> properties = new HashMap<String, Property>();
    private final Constructor<?> dbc;
    private final String[] constructorParamNames;

    public DescribableFactory(Class type) {
        this.type = type;

        constructorParamNames = new ClassDescriptor(type).loadConstructorParamNames();
        dbc = findConstructor(type);
        Type[] pt = dbc.getGenericParameterTypes();

        for (int i=0; i<pt.length; i++) {
            properties.put(constructorParamNames[i],new Property(constructorParamNames[i],pt[i]));
        }
        for (Class c=type; c!=null; c=c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.isAnnotationPresent(DataBoundSetter.class)) {
                    String n = Introspector.decapitalize(m.getName().substring(3));
                    properties.put(n,new Property(n,m.getParameterTypes()[0]));
                }
            }
            for (Field f : c.getDeclaredFields()) {
                if (f.isAnnotationPresent(DataBoundSetter.class)) {
                    String n = f.getName();
                    properties.put(n,new Property(n,f.getType()));
                }
            }
        }
    }

    private Constructor<?> findConstructor(Class type) {
        Constructor<?>[] ctrs = type.getConstructors();
        // which constructor was data bound?
        Constructor<?> dbc = null;
        for (Constructor<?> c : ctrs) {
            if (c.getAnnotation(DataBoundConstructor.class) != null) {
                dbc = c;
                break;
            }
        }

        if (dbc==null)
            throw new NoStaplerConstructorException("There's no @DataBoundConstructor on any constructor of " + type);
        return dbc;
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

    /*package*/ Object instantiate() {
        Object[] constructorArgs = new Object[constructorParamNames.length];
        for (int i=0; i<constructorParamNames.length; i++) {
            constructorArgs[i] = properties.get(constructorParamNames[i]).pack();
        }
        // TODO: handle setters
        try {
            return dbc.newInstance(constructorArgs);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to instantiate "+type,e);
        }
    }
}
