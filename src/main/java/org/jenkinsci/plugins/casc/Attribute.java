package org.jenkinsci.plugins.casc;

import org.apache.commons.beanutils.PropertyUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * One attribute of {@link Configurator}.
 *
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 * @see Configurator#describe()
 */
public class Attribute<T> {

    private final static Logger logger = Logger.getLogger(Attribute.class.getName());

    protected final String name;
    protected final Class type;
    private boolean multiple;
    protected String preferredName;
    private Setter setter = DEFAULT_SETTER;

    public Attribute(String name, Class type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return preferredName != null ? preferredName : name;
    }

    public Class getType() {
        return type;
    }

    /** Attribute acutaly is a Collection of documented type */
    public boolean isMultiple() {
        return multiple;
    }

    public Attribute<T> multiple(boolean multiple) {
        this.multiple = multiple;
        return this;
    }

    public Attribute<T> preferredName(String preferredName) {
        this.preferredName = preferredName;
        return this;
    }

    public Attribute<T> setter(Setter setter) {
        this.setter = setter;
        return this;
    }

    public Setter getSetter() {
        return setter;
    }

    /** If this attribute is constrained to a limited set of value, here they are */
    public List<String> possibleValues() {
        if (type.isEnum()) {
            Class<Enum> e = (Class<Enum>) type;
            return Arrays.stream(e.getEnumConstants()).map(Enum::name).collect(Collectors.toList());
        }
        return Collections.EMPTY_LIST;
    }


    public void setValue(T target, Object value) throws Exception {
        setter.setValue(target, this, value);
    }

    /**
     * Abstracts away how to assign a value to a 'target' Jenkins object.
     */
    @FunctionalInterface
    public interface Setter {
        void setValue(Object target, Attribute attribute, Object value) throws Exception;
    }

    /**
     * Default Setter implementation based on JavaBean property write method.
     */
    private static final Setter DEFAULT_SETTER = (target, attribute, value) -> {
        final String setterId = target.getClass().getCanonicalName()+'#'+attribute.name;
        logger.info("Setting " + setterId + " = " + value);
        final PropertyDescriptor property = PropertyUtils.getPropertyDescriptor(target, attribute.name);
        if (property == null) {
            throw new Exception("Default value setter cannot find Property Descriptor for " + setterId);
        }
        final Method writeMethod = property.getWriteMethod();

        Object o = value;
        if (attribute.multiple) {
            if (!(value instanceof Collection)) {
                throw new IllegalArgumentException(setterId + " should be a list.");
            }
            // if setter expect an Array, convert Collection to expected array type
            // Typically required for hudson.tools.ToolDescriptor.setInstallations
            // as java varargs unfortunately only supports Arrays, not all Iterable (sic)
            final Class c = writeMethod.getParameterTypes()[0];
            if (c.isArray()) {
                Collection collection = (Collection) value;
                o = collection.toArray((Object[]) Array.newInstance(attribute.type, collection.size()));

                // if setter expect a Set, convert Collection to Set
                // see jenkins.agentProtocols
            } else if(c.isAssignableFrom(Set.class)){
                o = new HashSet((Collection)value);
            }
        }

        writeMethod.invoke(target, o);
    };

}
