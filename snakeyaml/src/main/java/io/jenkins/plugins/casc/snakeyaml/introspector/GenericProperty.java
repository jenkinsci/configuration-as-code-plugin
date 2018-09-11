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

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

abstract public class GenericProperty extends Property {

    private Type genType;

    public GenericProperty(String name, Class<?> aClass, Type aType) {
        super(name, aClass);
        genType = aType;
        actualClassesChecked = aType == null;
    }

    private boolean actualClassesChecked;
    private Class<?>[] actualClasses;

    public Class<?>[] getActualTypeArguments() { // should we synchronize here ?
        if (!actualClassesChecked) {
            if (genType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) genType;
                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                if (actualTypeArguments.length > 0) {
                    actualClasses = new Class<?>[actualTypeArguments.length];
                    for (int i = 0; i < actualTypeArguments.length; i++) {
                        if (actualTypeArguments[i] instanceof Class<?>) {
                            actualClasses[i] = (Class<?>) actualTypeArguments[i];
                        } else if (actualTypeArguments[i] instanceof ParameterizedType) {
                            actualClasses[i] = (Class<?>) ((ParameterizedType) actualTypeArguments[i])
                                    .getRawType();
                        } else if (actualTypeArguments[i] instanceof GenericArrayType) {
                            Type componentType = ((GenericArrayType) actualTypeArguments[i])
                                    .getGenericComponentType();
                            if (componentType instanceof Class<?>) {
                                actualClasses[i] = Array.newInstance((Class<?>) componentType, 0)
                                        .getClass();
                            } else {
                                actualClasses = null;
                                break;
                            }
                        } else {
                            actualClasses = null;
                            break;
                        }
                    }
                }
            } else if (genType instanceof GenericArrayType) {
                Type componentType = ((GenericArrayType) genType).getGenericComponentType();
                if (componentType instanceof Class<?>) {
                    actualClasses = new Class<?>[] { (Class<?>) componentType };
                }
            } else if (genType instanceof Class<?>) {// XXX this check is only
                                                     // required for IcedTea6
                Class<?> classType = (Class<?>) genType;
                if (classType.isArray()) {
                    actualClasses = new Class<?>[1];
                    actualClasses[0] = getType().getComponentType();
                }
            }
            actualClassesChecked = true;
        }
        return actualClasses;
    }
}
