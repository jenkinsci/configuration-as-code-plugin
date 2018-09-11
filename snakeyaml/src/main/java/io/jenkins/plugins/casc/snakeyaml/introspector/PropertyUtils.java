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
package io.jenkins.plugins.casc.snakeyaml.introspector;

import java.beans.FeatureDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import io.jenkins.plugins.casc.snakeyaml.error.YAMLException;
import io.jenkins.plugins.casc.snakeyaml.util.PlatformFeatureDetector;

public class PropertyUtils {

    private final Map<Class<?>, Map<String, Property>> propertiesCache = new HashMap<Class<?>, Map<String, Property>>();
    private final Map<Class<?>, Set<Property>> readableProperties = new HashMap<Class<?>, Set<Property>>();
    private BeanAccess beanAccess = BeanAccess.DEFAULT;
    private boolean allowReadOnlyProperties = false;
    private boolean skipMissingProperties = false;

    private PlatformFeatureDetector platformFeatureDetector;

    public PropertyUtils() {
        this(new PlatformFeatureDetector());
    }

    PropertyUtils(PlatformFeatureDetector platformFeatureDetector) {
        this.platformFeatureDetector = platformFeatureDetector;

        /*
         * Android lacks much of java.beans (including the Introspector class, used here), because java.beans classes tend to rely on java.awt, which isn't
         * supported in the Android SDK. That means we have to fall back on FIELD access only when SnakeYAML is running on the Android Runtime.
         */
        if (platformFeatureDetector.isRunningOnAndroid()) {
            beanAccess = BeanAccess.FIELD;
        }
    }

    protected Map<String, Property> getPropertiesMap(Class<?> type, BeanAccess bAccess) {
        if (propertiesCache.containsKey(type)) {
            return propertiesCache.get(type);
        }

        Map<String, Property> properties = new LinkedHashMap<String, Property>();
        boolean inaccessableFieldsExist = false;
        switch (bAccess) {
            case FIELD:
                for (Class<?> c = type; c != null; c = c.getSuperclass()) {
                    for (Field field : c.getDeclaredFields()) {
                        int modifiers = field.getModifiers();
                        if (!Modifier.isStatic(modifiers) && !Modifier.isTransient(modifiers)
                                && !properties.containsKey(field.getName())) {
                            properties.put(field.getName(), new FieldProperty(field));
                        }
                    }
                }
                break;
            default:
                // add JavaBean properties
                try {
                    for (PropertyDescriptor property : Introspector.getBeanInfo(type)
                            .getPropertyDescriptors()) {
                        Method readMethod = property.getReadMethod();
                        if ((readMethod == null || !readMethod.getName().equals("getClass"))
                                && !isTransient(property)) {
                            properties.put(property.getName(), new MethodProperty(property));
                        }
                    }
                } catch (IntrospectionException e) {
                    throw new YAMLException(e);
                }

                // add public fields
                for (Class<?> c = type; c != null; c = c.getSuperclass()) {
                    for (Field field : c.getDeclaredFields()) {
                        int modifiers = field.getModifiers();
                        if (!Modifier.isStatic(modifiers) && !Modifier.isTransient(modifiers)) {
                            if (Modifier.isPublic(modifiers)) {
                                properties.put(field.getName(), new FieldProperty(field));
                            } else {
                                inaccessableFieldsExist = true;
                            }
                        }
                    }
                }
                break;
        }
        if (properties.isEmpty() && inaccessableFieldsExist) {
            throw new YAMLException("No JavaBean properties found in " + type.getName());
        }
        propertiesCache.put(type, properties);
        return properties;
    }

    private static final String TRANSIENT = "transient";

    private boolean isTransient(FeatureDescriptor fd) {
        return Boolean.TRUE.equals(fd.getValue(TRANSIENT));
    }

    public Set<Property> getProperties(Class<? extends Object> type) {
        return getProperties(type, beanAccess);
    }

    public Set<Property> getProperties(Class<? extends Object> type, BeanAccess bAccess) {
        if (readableProperties.containsKey(type)) {
            return readableProperties.get(type);
        }
        Set<Property> properties = createPropertySet(type, bAccess);
        readableProperties.put(type, properties);
        return properties;
    }

    protected Set<Property> createPropertySet(Class<? extends Object> type, BeanAccess bAccess) {
        Set<Property> properties = new TreeSet<Property>();
        Collection<Property> props = getPropertiesMap(type, bAccess).values();
        for (Property property : props) {
            if (property.isReadable() && (allowReadOnlyProperties || property.isWritable())) {
                properties.add(property);
            }
        }
        return properties;
    }

    public Property getProperty(Class<? extends Object> type, String name) {
        return getProperty(type, name, beanAccess);
    }

    public Property getProperty(Class<? extends Object> type, String name, BeanAccess bAccess) {
        Map<String, Property> properties = getPropertiesMap(type, bAccess);
        Property property = properties.get(name);
        if (property == null && skipMissingProperties) {
            property = new MissingProperty(name);
        }
        if (property == null) {
            throw new YAMLException(
                    "Unable to find property '" + name + "' on class: " + type.getName());
        }
        return property;
    }

    public void setBeanAccess(BeanAccess beanAccess) {
        if (platformFeatureDetector.isRunningOnAndroid() && beanAccess != BeanAccess.FIELD) {
            throw new IllegalArgumentException(
                    "JVM is Android - only BeanAccess.FIELD is available");
        }

        if (this.beanAccess != beanAccess) {
            this.beanAccess = beanAccess;
            propertiesCache.clear();
            readableProperties.clear();
        }
    }

    public void setAllowReadOnlyProperties(boolean allowReadOnlyProperties) {
        if (this.allowReadOnlyProperties != allowReadOnlyProperties) {
            this.allowReadOnlyProperties = allowReadOnlyProperties;
            readableProperties.clear();
        }
    }

    public boolean isAllowReadOnlyProperties() {
        return allowReadOnlyProperties;
    }

    /**
     * Skip properties that are missing during deserialization of YAML to a Java
     * object. The default is false.
     *
     * @param skipMissingProperties
     *            true if missing properties should be skipped, false otherwise.
     */
    public void setSkipMissingProperties(boolean skipMissingProperties) {
        if (this.skipMissingProperties != skipMissingProperties) {
            this.skipMissingProperties = skipMissingProperties;
            readableProperties.clear();
        }
    }

    public boolean isSkipMissingProperties() {
        return skipMissingProperties;
    }
}
