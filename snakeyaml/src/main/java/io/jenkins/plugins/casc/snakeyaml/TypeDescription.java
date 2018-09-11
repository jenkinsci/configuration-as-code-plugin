/**
 * Copyright (c) 2008, http://www.snakeyaml.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.jenkins.plugins.casc.snakeyaml;

import java.util.Collection;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import io.jenkins.plugins.casc.snakeyaml.error.YAMLException;
import io.jenkins.plugins.casc.snakeyaml.introspector.BeanAccess;
import io.jenkins.plugins.casc.snakeyaml.introspector.Property;
import io.jenkins.plugins.casc.snakeyaml.introspector.PropertySubstitute;
import io.jenkins.plugins.casc.snakeyaml.introspector.PropertyUtils;
import io.jenkins.plugins.casc.snakeyaml.nodes.Node;
import io.jenkins.plugins.casc.snakeyaml.nodes.Tag;

/**
 * Provides additional runtime information necessary to create a custom Java
 * instance.
 *
 * In general this class is thread-safe and can be used as a singleton, the only
 * exception being the PropertyUtils field. A singleton PropertyUtils should be
 * constructed and shared between all YAML Constructors used if a singleton
 * TypeDescription is used, since Constructor sets its propertyUtils to the
 * TypeDescription that is passed to it, hence you may end up in a situation
 * when propertyUtils in TypeDescription is from different Constructor.
 */
public class TypeDescription {
    final private static Logger log = Logger
            .getLogger(TypeDescription.class.getPackage().getName());

    private final Class<? extends Object> type;

    // class that implements the described type; if set, will be used as a source for constructor.
    // If not set - TypeDescription will leave instantiation of an entity to the YAML Constructor
    private Class<?> impl;

    private Tag tag;

    transient private Set<Property> dumpProperties;
    transient private PropertyUtils propertyUtils;
    transient private boolean delegatesChecked;

    private Map<String, PropertySubstitute> properties = Collections.emptyMap();

    protected Set<String> excludes = Collections.emptySet();
    protected String[] includes = null;
    protected BeanAccess beanAccess;

    public TypeDescription(Class<? extends Object> clazz, Tag tag) {
        this(clazz, tag, null);
    }

    public TypeDescription(Class<? extends Object> clazz, Tag tag, Class<?> impl) {
        this.type = clazz;
        this.tag = tag;
        this.impl = impl;
        beanAccess = null;
    }

    public TypeDescription(Class<? extends Object> clazz, String tag) {
        this(clazz, new Tag(tag), null);
    }

    public TypeDescription(Class<? extends Object> clazz) {
        this(clazz, (Tag) null, null);
    }

    public TypeDescription(Class<? extends Object> clazz, Class<?> impl) {
        this(clazz, null, impl);
    }

    /**
     * Get tag which shall be used to load or dump the type (class).
     *
     * @return tag to be used. It may be a tag for Language-Independent Types
     *         (http://www.yaml.org/type/)
     */
    public Tag getTag() {
        return tag;
    }

    /**
     * Set tag to be used to load or dump the type (class).
     *
     * @param tag
     *            local or global tag
     */
    public void setTag(Tag tag) {
        this.tag = tag;
    }

    public void setTag(String tag) {
        setTag(new Tag(tag));
    }

    /**
     * Get represented type (class)
     *
     * @return type (class) to be described.
     */
    public Class<? extends Object> getType() {
        return type;
    }

    /**
     * Specify that the property is a type-safe <code>List</code>.
     *
     * @param property
     *            name of the JavaBean property
     * @param type
     *            class of List values
     */
    @Deprecated
    public void putListPropertyType(String property, Class<? extends Object> type) {
        addPropertyParameters(property, type);
    }

    /**
     * Get class of List values for provided JavaBean property.
     *
     * @param property
     *            property name
     * @return class of List values
     */
    @Deprecated
    public Class<? extends Object> getListPropertyType(String property) {
        if (properties.containsKey(property)) {
            Class<?>[] typeArguments = properties.get(property).getActualTypeArguments();
            if (typeArguments != null && typeArguments.length > 0) {
                return typeArguments[0];
            }
        }
        return null;
    }

    /**
     * Specify that the property is a type-safe <code>Map</code>.
     *
     * @param property
     *            property name of this JavaBean
     * @param key
     *            class of keys in Map
     * @param value
     *            class of values in Map
     */
    @Deprecated
    public void putMapPropertyType(String property, Class<? extends Object> key,
            Class<? extends Object> value) {
        addPropertyParameters(property, key, value);
    }

    /**
     * Get keys type info for this JavaBean
     *
     * @param property
     *            property name of this JavaBean
     * @return class of keys in the Map
     */
    @Deprecated
    public Class<? extends Object> getMapKeyType(String property) {
        if (properties.containsKey(property)) {
            Class<?>[] typeArguments = properties.get(property).getActualTypeArguments();
            if (typeArguments != null && typeArguments.length > 0) {
                return typeArguments[0];
            }
        }
        return null;
    }

    /**
     * Get values type info for this JavaBean
     *
     * @param property
     *            property name of this JavaBean
     * @return class of values in the Map
     */
    @Deprecated
    public Class<? extends Object> getMapValueType(String property) {
        if (properties.containsKey(property)) {
            Class<?>[] typeArguments = properties.get(property).getActualTypeArguments();
            if (typeArguments != null && typeArguments.length > 1) {
                return typeArguments[1];
            }
        }
        return null;
    }

    /**
     * Adds new substitute for property <code>pName</code> parameterized by
     * <code>classes</code> to this <code>TypeDescription</code>. If
     * <code>pName</code> has been added before - updates parameters with
     * <code>classes</code>.
     *
     * @param pName - parameter name
     * @param classes - parameterized by
     */
    public void addPropertyParameters(String pName, Class<?>... classes) {
        if (!properties.containsKey(pName)) {
            substituteProperty(pName, null, null, null, classes);
        } else {
            PropertySubstitute pr = properties.get(pName);
            pr.setActualTypeArguments(classes);
        }

    }

    @Override
    public String toString() {
        return "TypeDescription for " + getType() + " (tag='" + getTag() + "')";
    }

    private void checkDelegates() {
        Collection<PropertySubstitute> values = properties.values();
        for (PropertySubstitute p : values) {
            try {
                p.setDelegate(discoverProperty(p.getName()));
            } catch (YAMLException e) {
            }
        }
        delegatesChecked = true;
    }

    private Property discoverProperty(String name) {
        if (propertyUtils != null) {
            if (beanAccess == null) {
                return propertyUtils.getProperty(type, name);
            }
            return propertyUtils.getProperty(type, name, beanAccess);
        }
        return null;
    }

    public Property getProperty(String name) {
        if (!delegatesChecked) {
            checkDelegates();
        }
        return properties.containsKey(name) ? properties.get(name) : discoverProperty(name);
    }

    /**
     * Adds property substitute for <code>pName</code>
     *
     * @param pName
     *            property name
     * @param pType
     *            property type
     * @param getter
     *            method name for getter
     * @param setter
     *            method name for setter
     * @param argParams
     *            actual types for parameterized type (List&lt;?&gt;, Map&lt;?&gt;)
     */
    public void substituteProperty(String pName, Class<?> pType, String getter, String setter,
            Class<?>... argParams) {
        substituteProperty(new PropertySubstitute(pName, pType, getter, setter, argParams));
    }

    public void substituteProperty(PropertySubstitute substitute) {
        if (Collections.EMPTY_MAP == properties) {
            properties = new LinkedHashMap<String, PropertySubstitute>();
        }
        substitute.setTargetType(type);
        properties.put(substitute.getName(), substitute);
    }

    public void setPropertyUtils(PropertyUtils propertyUtils) {
        this.propertyUtils = propertyUtils;
    }

    /* begin: Representer */
    public void setIncludes(String... propNames) {
        this.includes = (propNames != null && propNames.length > 0) ? propNames : null;
    }

    public void setExcludes(String... propNames) {
        if (propNames != null && propNames.length > 0) {
            excludes = new HashSet<String>();
            for (String name : propNames) {
                excludes.add(name);
            }
        } else {
            excludes = Collections.emptySet();
        }
    }

    public Set<Property> getProperties() {
        if (dumpProperties != null) {
            return dumpProperties;
        }

        if (propertyUtils != null) {
            if (includes != null) {
                dumpProperties = new LinkedHashSet<Property>();
                for (String propertyName : includes) {
                    if (!excludes.contains(propertyName)) {
                        dumpProperties.add(getProperty(propertyName));
                    }
                }
                return dumpProperties;
            }

            final Set<Property> readableProps = (beanAccess == null)
                    ? propertyUtils.getProperties(type)
                    : propertyUtils.getProperties(type, beanAccess);

            if (properties.isEmpty()) {
                if (excludes.isEmpty()) {
                    return dumpProperties = readableProps;
                }
                dumpProperties = new LinkedHashSet<Property>();
                for (Property property : readableProps) {
                    if (!excludes.contains(property.getName())) {
                        dumpProperties.add(property);
                    }
                }
                return dumpProperties;
            }

            if (!delegatesChecked) {
                checkDelegates();
            }

            dumpProperties = new LinkedHashSet<Property>();

            for (Property property : properties.values()) {
                if (!excludes.contains(property.getName()) && property.isReadable()) {
                    dumpProperties.add(property);
                }
            }

            for (Property property : readableProps) {
                if (!excludes.contains(property.getName())) {
                    dumpProperties.add(property);
                }
            }

            return dumpProperties;
        }
        return null;
    }

    /* end: Representer */

    /*------------ Maybe something useful to override :) ---------*/

    public boolean setupPropertyType(String key, Node valueNode) {
        return false;
    }

    public boolean setProperty(Object targetBean, String propertyName, Object value)
            throws Exception {
        return false;
    }

    /**
     * This method should be overridden for TypeDescription implementations that are supposed to implement
     * instantiation logic that is different from default one as implemented in YAML constructors.
     * Note that even if you override this method, default filling of fields with
     * variables from parsed YAML will still occur later.
     * @param node - node to construct the instance from
     * @return new instance
     */
    public Object newInstance(Node node) {
        if (impl != null) {
            try {
                java.lang.reflect.Constructor<?> c = impl.getDeclaredConstructor();
                c.setAccessible(true);
                return c.newInstance();
            } catch (Exception e) {
                log.fine(e.getLocalizedMessage());
                impl = null;
            }
        }
        return null;
    }

    public Object newInstance(String propertyName, Node node) {
        return null;
    }

    /**
     * Is invoked after entity is filled with values from deserialized YAML
     * @param obj - deserialized entity
     * @return postprocessed deserialized entity
     */
    public Object finalizeConstruction(Object obj) {
        return obj;
    }

}
