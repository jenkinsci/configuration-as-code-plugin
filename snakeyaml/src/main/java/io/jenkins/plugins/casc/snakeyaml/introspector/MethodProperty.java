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

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.util.List;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import io.jenkins.plugins.casc.snakeyaml.error.YAMLException;
import io.jenkins.plugins.casc.snakeyaml.util.ArrayUtils;

/**
 * <p>
 * A <code>MethodProperty</code> is a <code>Property</code> which is accessed
 * through accessor methods (setX, getX). It is possible to have a
 * <code>MethodProperty</code> which has only setter, only getter, or both. It
 * is not possible to have a <code>MethodProperty</code> which has neither
 * setter nor getter.
 * </p>
 */
public class MethodProperty extends GenericProperty {

    private final PropertyDescriptor property;
    private final boolean readable;
    private final boolean writable;

    private static Type discoverGenericType(PropertyDescriptor property) {
        Method readMethod = property.getReadMethod();
        if (readMethod != null) {
            return readMethod.getGenericReturnType();
        }

        Method writeMethod = property.getWriteMethod();
        if (writeMethod != null) {
            Type[] paramTypes = writeMethod.getGenericParameterTypes();
            if (paramTypes.length > 0) {
                return paramTypes[0];
            }
        }
        /*
         * This actually may happen if PropertyDescriptor is of type
         * IndexedPropertyDescriptor and it has only IndexedGetter/Setter. ATM
         * we simply skip type discovery.
         */
        return null;
    }

    public MethodProperty(PropertyDescriptor property) {
        super(property.getName(), property.getPropertyType(),
                MethodProperty.discoverGenericType(property));
        this.property = property;
        this.readable = property.getReadMethod() != null;
        this.writable = property.getWriteMethod() != null;
    }

    @Override
    public void set(Object object, Object value) throws Exception {
        if (!writable) {
            throw new YAMLException("No writable property '" + getName() + "' on class: "
                    + object.getClass().getName());
        }
        property.getWriteMethod().invoke(object, value);
    }

    @Override
    public Object get(Object object) {
        try {
            property.getReadMethod().setAccessible(true);// issue 50
            return property.getReadMethod().invoke(object);
        } catch (Exception e) {
            throw new YAMLException("Unable to find getter for property '" + property.getName()
                    + "' on object " + object + ":" + e);
        }
    }

    /**
     * Returns the annotations that are present on read and write methods of this property or empty {@code List} if
     * there're no annotations.
     *
     * @return the annotations that are present on this property or empty {@code List} if there're no annotations
     */
    @Override
    public List<Annotation> getAnnotations() {
        List<Annotation> annotations;
        if (isReadable() && isWritable()) {
            annotations = ArrayUtils.toUnmodifiableCompositeList(property.getReadMethod().getAnnotations(), property.getWriteMethod().getAnnotations());
        } else if (isReadable()) {
            annotations = ArrayUtils.toUnmodifiableList(property.getReadMethod().getAnnotations());
        } else {
            annotations = ArrayUtils.toUnmodifiableList(property.getWriteMethod().getAnnotations());
        }
        return annotations;
    }

    /**
     * Returns property's annotation for the given type or {@code null} if it's not present. If the annotation is present
     * on both read and write methods, the annotation on read method takes precedence.
     *
     * @param annotationType the type of the annotation to be returned
     * @return property's annotation for the given type or {@code null} if it's not present
     */
    @Override
    public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
        A annotation = null;
        if (isReadable()) {
            annotation = property.getReadMethod().getAnnotation(annotationType);
        }
        if (annotation == null && isWritable()) {
            annotation = property.getWriteMethod().getAnnotation(annotationType);
        }
        return annotation;
    }

    @Override
    public boolean isWritable() {
        return writable;
    }

    @Override
    public boolean isReadable() {
        return readable;
    }

}