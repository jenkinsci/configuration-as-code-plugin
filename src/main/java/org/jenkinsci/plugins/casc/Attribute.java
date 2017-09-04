package org.jenkinsci.plugins.casc;

import org.apache.commons.beanutils.PropertyUtils;
import org.kohsuke.stapler.lang.Klass;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class Attribute<T> {

    protected final String name;
    protected final Class<T> type;
    private boolean multiple;

    public Attribute(String name, Class<T> type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public Class<T> getType() {
        return type;
    }

    /** Attribute acutaly is a Collection of documented type */
    public boolean isMultiple() {
        return multiple;
    }

    public Attribute<T> withMultiple(boolean multiple) {
        this.multiple = multiple;
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
        PropertyUtils.setProperty(target, name, value);
    }
}
