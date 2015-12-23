package org.jenkinsci.plugins.systemconfigdsl;

import org.kohsuke.stapler.ClassDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.NoStaplerConstructorException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;

/**
 * Closure delegate that instantiates a data-bindable object
 * via {@link DataBoundConstructor}.
 *
 * <p>
 * Constructor arguments and setter arguments are equally treated in the syntax.
 *
 * @author Kohsuke Kawaguchi
 */
public class DescribableFactory extends PropertyBuilder {
    private final Constructor<?> dbc;
    private final String[] constructorParamNames;

    public DescribableFactory(Class type) {
        super(type);

        constructorParamNames = new ClassDescriptor(type).loadConstructorParamNames();
        dbc = findConstructor(type);
        Type[] pt = dbc.getGenericParameterTypes();

        for (int i=0; i<pt.length; i++) {
            properties.put(constructorParamNames[i],new Property(constructorParamNames[i],pt[i],null));
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


    /*package*/ Object instantiate() {
        Object[] constructorArgs = new Object[constructorParamNames.length];
        for (int i=0; i<constructorParamNames.length; i++) {
            constructorArgs[i] = properties.get(constructorParamNames[i]).pack();
        }
        try {
            Object o = dbc.newInstance(constructorArgs);
            handleSetters(o);
            return o;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to instantiate "+type,e);
        }
    }
}
