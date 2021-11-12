/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.jenkins.plugins.casc.util;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.lang.reflect.Field;
import java.util.Iterator;
import org.apache.commons.lang.ClassUtils;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * Extends {@link org.apache.commons.lang.reflect.FieldUtils} by adding
 * some utility methods.
 */
@Restricted(NoExternalUse.class)
public class ExtraFieldUtils extends org.apache.commons.lang.reflect.FieldUtils {

    /**
     * Gets an accessible <code>Field</code> by name without breaking scope.
     * Superclasses/interfaces will be considered.
     * This is an equivalent of {@link #getField(Class, String, boolean)} from the commons lang library,
     * but without {@link Field#setAccessible(boolean)} invocation.
     *
     * @param cls  the class to reflect, must not be null
     * @param fieldName  the field name to obtain
     * @return the Field object, it might be {@code null}
     * @throws IllegalArgumentException if the class or field name is null
     */
    @CheckForNull
    public static Field getFieldNoForce(final Class cls, String fieldName) {
        if (cls == null) {
            throw new IllegalArgumentException("The class must not be null");
        }
        if (fieldName == null) {
            throw new IllegalArgumentException("The field name must not be null");
        }
        // Sun Java 1.3 has a bugged implementation of getField hence we write the
        // code ourselves

        // getField() will return the Field object with the declaring class
        // set correctly to the class that declares the field. Thus requesting the
        // field on a subclass will return the field from the superclass.
        //
        // priority order for lookup:
        // searchclass private/protected/package/public
        // superclass protected/package/public
        //  private/different package blocks access to further superclasses
        // implementedinterface public

        // check up the superclass hierarchy
        for (Class acls = cls; acls != null; acls = acls.getSuperclass()) {
            try {
                Field field = acls.getDeclaredField(fieldName);
                // getDeclaredField checks for non-public scopes, and we return it to upstream
                return field;
            } catch (NoSuchFieldException ex) {
                // ignore
            }
        }
        // check the public interface case. This must be manually searched for
        // incase there is a public supersuperclass field hidden by a private/package
        // superclass field.
        Field match = null;
        for (Iterator intf = ClassUtils.getAllInterfaces(cls).iterator(); intf
                .hasNext();) {
            try {
                Field test = ((Class) intf.next()).getField(fieldName);
                if (match != null) {
                    throw new IllegalArgumentException(
                            "Reference to field "
                                    + fieldName
                                    + " is ambiguous relative to "
                                    + cls
                                    + "; a matching field exists on two or more implemented interfaces.");
                }
                match = test;
            } catch (NoSuchFieldException ex) {
                // ignore
            }
        }
        return match;
    }

}
