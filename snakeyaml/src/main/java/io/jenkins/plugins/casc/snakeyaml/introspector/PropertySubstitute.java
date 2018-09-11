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

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.jenkins.plugins.casc.snakeyaml.error.YAMLException;

// TODO: decide priorities for get/set Read/Field/Delegate Write/Field/Delegate - is FIELD on the correct place ?
public class PropertySubstitute extends Property {

    final private static Logger log = Logger.getLogger(PropertySubstitute.class.getPackage()
            .getName());

    protected Class<?> targetType;
    private final String readMethod;
    private final String writeMethod;
    transient private Method read;
    transient private Method write;
    private Field field;
    protected Class<?>[] parameters;
    private Property delegate;
    private boolean filler;

    public PropertySubstitute(String name, Class<?> type, String readMethod, String writeMethod,
            Class<?>... params) {
        super(name, type);
        this.readMethod = readMethod;
        this.writeMethod = writeMethod;
        setActualTypeArguments(params);
        this.filler = false;
    }

    public PropertySubstitute(String name, Class<?> type, Class<?>... params) {
        this(name, type, null, null, params);
    }

    @Override
    public Class<?>[] getActualTypeArguments() {
        if (parameters == null && delegate != null) {
            return delegate.getActualTypeArguments();
        }
        return parameters;
    }

    public void setActualTypeArguments(Class<?>... args) {
        if (args != null && args.length > 0) {
            parameters = args;
        } else {
            parameters = null;
        }
    }

    @Override
    public void set(Object object, Object value) throws Exception {
        if (write != null) {
            if (!filler) {
                write.invoke(object, value);
            } else if (value != null) {
                if (value instanceof Collection<?>) {
                    Collection<?> collection = (Collection<?>) value;
                    for (Object val : collection) {
                        write.invoke(object, val);
                    }
                } else if (value instanceof Map<?, ?>) {
                    Map<?, ?> map = (Map<?, ?>) value;
                    for (Entry<?, ?> entry : map.entrySet()) {
                        write.invoke(object, entry.getKey(), entry.getValue());
                    }
                } else if (value.getClass().isArray()) { // TODO: maybe arrays
                                                         // need 2 fillers like
                                                         // SET(index, value)
                                                         // add ADD(value)
                    int len = Array.getLength(value);
                    for (int i = 0; i < len; i++) {
                        write.invoke(object, Array.get(value, i));
                    }
                }
            }
        } else if (field != null) {
            field.set(object, value);
        } else if (delegate != null) {
            delegate.set(object, value);
        } else {
            log.warning("No setter/delegate for '" + getName() + "' on object " + object);
        }
        // TODO: maybe throw YAMLException here
    }

    @Override
    public Object get(Object object) {
        try {
            if (read != null) {
                return read.invoke(object);
            } else if (field != null) {
                return field.get(object);
            }
        } catch (Exception e) {
            throw new YAMLException("Unable to find getter for property '" + getName()
                    + "' on object " + object + ":" + e);
        }

        if (delegate != null) {
            return delegate.get(object);
        }
        throw new YAMLException("No getter or delegate for property '" + getName() + "' on object "
                + object);
    }

    @Override
    public List<Annotation> getAnnotations() {
        Annotation[] annotations = null;
        if (read != null) {
            annotations = read.getAnnotations();
        } else if (field != null) {
            annotations = field.getAnnotations();
        }
        return annotations != null ? Arrays.asList(annotations) : delegate.getAnnotations();
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
        A annotation;
        if (read != null) {
            annotation = read.getAnnotation(annotationType);
        } else if (field != null) {
            annotation = field.getAnnotation(annotationType);
        } else {
            annotation = delegate.getAnnotation(annotationType);
        }
        return annotation;
    }

    public void setTargetType(Class<?> targetType) {
        if (this.targetType != targetType) {
            this.targetType = targetType;

            final String name = getName();
            for (Class<?> c = targetType; c != null; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    if (f.getName().equals(name)) {
                        int modifiers = f.getModifiers();
                        if (!Modifier.isStatic(modifiers) && !Modifier.isTransient(modifiers)) {
                            f.setAccessible(true);
                            field = f;
                        }
                        break;
                    }
                }
            }
            if (field == null && log.isLoggable(Level.FINE)) {
                log.fine(String.format("Failed to find field for %s.%s", targetType.getName(),
                        getName()));
            }

            // Retrieve needed info
            if (readMethod != null) {
                read = discoverMethod(targetType, readMethod);
            }
            if (writeMethod != null) {
                filler = false;
                write = discoverMethod(targetType, writeMethod, getType());
                if (write == null && parameters != null) {
                    filler = true;
                    write = discoverMethod(targetType, writeMethod, parameters);
                }
            }
        }
    }

    private Method discoverMethod(Class<?> type, String name, Class<?>... params) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            for (Method method : c.getDeclaredMethods()) {
                if (name.equals(method.getName())) {
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    if (parameterTypes.length != params.length) {
                        continue;
                    }
                    boolean found = true;
                    for (int i = 0; i < parameterTypes.length; i++) {
                        if (!parameterTypes[i].isAssignableFrom(params[i])) {
                            found = false;
                        }
                    }
                    if (found) {
                        method.setAccessible(true);
                        return method;
                    }
                }
            }
        }
        if (log.isLoggable(Level.FINE)) {
            log.fine(String.format("Failed to find [%s(%d args)] for %s.%s", name, params.length,
                    targetType.getName(), getName()));
        }
        return null;
    }

    @Override
    public String getName() {
        final String n = super.getName();
        if (n != null) {
            return n;
        }
        return delegate != null ? delegate.getName() : null;
    }

    @Override
    public Class<?> getType() {
        final Class<?> t = super.getType();
        if (t != null) {
            return t;
        }
        return delegate != null ? delegate.getType() : null;
    }

    @Override
    public boolean isReadable() {
        return (read != null) || (field != null) || (delegate != null && delegate.isReadable());
    }

    @Override
    public boolean isWritable() {
        return (write != null) || (field != null) || (delegate != null && delegate.isWritable());
    }

    public void setDelegate(Property delegate) {
        this.delegate = delegate;
        if (writeMethod != null && write == null && !filler) {
            filler = true;
            write = discoverMethod(targetType, writeMethod, getActualTypeArguments());
        }
    }
}
