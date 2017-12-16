package org.jenkinsci.plugins.casc;

import org.apache.commons.beanutils.PropertyUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class Attribute<T> {

    private final static Logger logger = Logger.getLogger(Attribute.class.getName());

    protected final String name;
    protected final Class<T> type;
    private boolean multiple;
    protected String preferredName;

    public Attribute(String name, Class<T> type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return preferredName != null ? preferredName : name;
    }

    public Class<T> getType() {
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


    /** If this attribute is constrained to a limited set of value, here they are */
    public List<String> possibleValues() {
        if (type.isEnum()) {
            Class<Enum> e = (Class<Enum>) type;
            return Arrays.stream(e.getEnumConstants()).map(Enum::name).collect(Collectors.toList());
        }
        return Collections.EMPTY_LIST;
    }


    public void setValue(Object target, T value) throws Exception {
        logger.info("Setting "+target.getClass().getCanonicalName()+'#'+name+" = " + value);
        final PropertyDescriptor property = PropertyUtils.getPropertyDescriptor(target, name);
        final Method writeMethod = property.getWriteMethod();

        Object o = value;
        if (multiple) {
            if (!(value instanceof Collection)) {
                throw new IllegalArgumentException(target + "#" + name + " should be a list.");
            }
            // if setter expect an Array, convert Collection to expected array type
            // Typically required for hudson.tools.ToolDescriptor.setInstallations
            // as java varargs unfortunately only supports Arrays, not all Iterable (sic)
            final Class c = writeMethod.getParameterTypes()[0];
            if (c.isArray() && value instanceof Collection) {
                Collection collection = (Collection) value;
                o = collection.toArray((Object[]) Array.newInstance(type, collection.size()));
            }

        }
        
        writeMethod.invoke(target, o);
    }

}
