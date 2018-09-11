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
package io.jenkins.plugins.casc.snakeyaml.constructor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import io.jenkins.plugins.casc.snakeyaml.TypeDescription;
import io.jenkins.plugins.casc.snakeyaml.error.YAMLException;
import io.jenkins.plugins.casc.snakeyaml.introspector.Property;
import io.jenkins.plugins.casc.snakeyaml.nodes.MappingNode;
import io.jenkins.plugins.casc.snakeyaml.nodes.Node;
import io.jenkins.plugins.casc.snakeyaml.nodes.NodeId;
import io.jenkins.plugins.casc.snakeyaml.nodes.NodeTuple;
import io.jenkins.plugins.casc.snakeyaml.nodes.ScalarNode;
import io.jenkins.plugins.casc.snakeyaml.nodes.SequenceNode;
import io.jenkins.plugins.casc.snakeyaml.nodes.Tag;

/**
 * Construct a custom Java instance.
 */
public class Constructor extends SafeConstructor {

    public Constructor() {
        this(Object.class);
    }

    /**
     * Create Constructor for the specified class as the root.
     *
     * @param theRoot
     *            - the class (usually JavaBean) to be constructed
     */
    public Constructor(Class<? extends Object> theRoot) {
        this(new TypeDescription(checkRoot(theRoot)));
    }

    /**
     * Ugly Java way to check the argument in the constructor
     */
    private static Class<? extends Object> checkRoot(Class<? extends Object> theRoot) {
        if (theRoot == null) {
            throw new NullPointerException("Root class must be provided.");
        } else
            return theRoot;
    }

    public Constructor(TypeDescription theRoot) {
        this(theRoot, null);
    }

    public Constructor(TypeDescription theRoot, Collection<TypeDescription> moreTDs) {
        if (theRoot == null) {
            throw new NullPointerException("Root type must be provided.");
        }
        this.yamlConstructors.put(null, new ConstructYamlObject());
        if (!Object.class.equals(theRoot.getType())) {
            rootTag = new Tag(theRoot.getType());
        }
        yamlClassConstructors.put(NodeId.scalar, new ConstructScalar());
        yamlClassConstructors.put(NodeId.mapping, new ConstructMapping());
        yamlClassConstructors.put(NodeId.sequence, new ConstructSequence());
        addTypeDescription(theRoot);
        if (moreTDs != null) {
            for (TypeDescription td : moreTDs) {
                addTypeDescription(td);
            }
        }
    }

    /**
     * Create Constructor for a class which does not have to be in the classpath
     * or for a definition from a Spring ApplicationContext.
     *
     * @param theRoot
     *            fully qualified class name of the root class (usually
     *            JavaBean)
     * @throws ClassNotFoundException if cannot be loaded by the classloader
     */
    public Constructor(String theRoot) throws ClassNotFoundException {
        this(Class.forName(check(theRoot)));
    }

    private static final String check(String s) {
        if (s == null) {
            throw new NullPointerException("Root type must be provided.");
        }
        if (s.trim().length() == 0) {
            throw new YAMLException("Root type must be provided.");
        }
        return s;
    }

    /**
     * Construct mapping instance (Map, JavaBean) when the runtime class is
     * known.
     */
    protected class ConstructMapping implements Construct {

        /**
         * Construct JavaBean. If type safe collections are used please look at
         * <code>TypeDescription</code>.
         *
         * @param node
         *            node where the keys are property names (they can only be
         *            <code>String</code>s) and values are objects to be created
         * @return constructed JavaBean
         */
        public Object construct(Node node) {
            MappingNode mnode = (MappingNode) node;
            if (Map.class.isAssignableFrom(node.getType())) {
                if (node.isTwoStepsConstruction()) {
                    return newMap(mnode);
                } else {
                    return constructMapping(mnode);
                }
            } else if (Collection.class.isAssignableFrom(node.getType())) {
                if (node.isTwoStepsConstruction()) {
                    return newSet(mnode);
                } else {
                    return constructSet(mnode);
                }
            } else {
                Object obj = Constructor.this.newInstance(mnode);
                if (node.isTwoStepsConstruction()) {
                    return obj;
                } else {
                    return constructJavaBean2ndStep(mnode, obj);
                }
            }
        }

        @SuppressWarnings("unchecked")
        public void construct2ndStep(Node node, Object object) {
            if (Map.class.isAssignableFrom(node.getType())) {
                constructMapping2ndStep((MappingNode) node, (Map<Object, Object>) object);
            } else if (Set.class.isAssignableFrom(node.getType())) {
                constructSet2ndStep((MappingNode) node, (Set<Object>) object);
            } else {
                constructJavaBean2ndStep((MappingNode) node, object);
            }
        }

        // protected Object createEmptyJavaBean(MappingNode node) {
        // try {
        // Object instance = Constructor.this.newInstance(node);
        // if (instance != null) {
        // return instance;
        // }
        //
        // /**
        // * Using only default constructor. Everything else will be
        // * initialized on 2nd step. If we do here some partial
        // * initialization, how do we then track what need to be done on
        // * 2nd step? I think it is better to get only object here (to
        // * have it as reference for recursion) and do all other thing on
        // * 2nd step.
        // */
        // java.lang.reflect.Constructor<?> c =
        // node.getType().getDeclaredConstructor();
        // c.setAccessible(true);
        // return c.newInstance();
        // } catch (Exception e) {
        // throw new YAMLException(e);
        // }
        // }

        protected Object constructJavaBean2ndStep(MappingNode node, Object object) {
            flattenMapping(node);
            Class<? extends Object> beanType = node.getType();
            List<NodeTuple> nodeValue = node.getValue();
            for (NodeTuple tuple : nodeValue) {
                ScalarNode keyNode;
                if (tuple.getKeyNode() instanceof ScalarNode) {
                    // key must be scalar
                    keyNode = (ScalarNode) tuple.getKeyNode();
                } else {
                    throw new YAMLException(
                            "Keys must be scalars but found: " + tuple.getKeyNode());
                }
                Node valueNode = tuple.getValueNode();
                // keys can only be Strings
                keyNode.setType(String.class);
                String key = (String) constructObject(keyNode);
                try {
                    TypeDescription memberDescription = typeDefinitions.get(beanType);
                    Property property = memberDescription == null ? getProperty(beanType, key)
                            : memberDescription.getProperty(key);

                    if (!property.isWritable()) {
                        throw new YAMLException("No writable property '" + key + "' on class: "
                                + beanType.getName());
                    }

                    valueNode.setType(property.getType());
                    final boolean typeDetected = (memberDescription != null)
                            ? memberDescription.setupPropertyType(key, valueNode)
                            : false;
                    if (!typeDetected && valueNode.getNodeId() != NodeId.scalar) {
                        // only if there is no explicit TypeDescription
                        Class<?>[] arguments = property.getActualTypeArguments();
                        if (arguments != null && arguments.length > 0) {
                            // type safe (generic) collection may contain the
                            // proper class
                            if (valueNode.getNodeId() == NodeId.sequence) {
                                Class<?> t = arguments[0];
                                SequenceNode snode = (SequenceNode) valueNode;
                                snode.setListType(t);
                            } else if (Set.class.isAssignableFrom(valueNode.getType())) {
                                Class<?> t = arguments[0];
                                MappingNode mnode = (MappingNode) valueNode;
                                mnode.setOnlyKeyType(t);
                                mnode.setUseClassConstructor(true);
                            } else if (Map.class.isAssignableFrom(valueNode.getType())) {
                                Class<?> ketType = arguments[0];
                                Class<?> valueType = arguments[1];
                                MappingNode mnode = (MappingNode) valueNode;
                                mnode.setTypes(ketType, valueType);
                                mnode.setUseClassConstructor(true);
                            }
                        }
                    }

                    Object value = (memberDescription != null)
                            ? newInstance(memberDescription, key, valueNode)
                            : constructObject(valueNode);
                    // Correct when the property expects float but double was
                    // constructed
                    if (property.getType() == Float.TYPE || property.getType() == Float.class) {
                        if (value instanceof Double) {
                            value = ((Double) value).floatValue();
                        }
                    }
                    // Correct when the property a String but the value is binary
                    if (property.getType() == String.class && Tag.BINARY.equals(valueNode.getTag())
                            && value instanceof byte[]) {
                        value = new String((byte[]) value);
                    }

                    if (memberDescription == null
                            || !memberDescription.setProperty(object, key, value)) {
                        property.set(object, value);
                    }
                } catch (DuplicateKeyException e) {
                    throw e;
                } catch (Exception e) {
                    throw new ConstructorException(
                            "Cannot create property=" + key + " for JavaBean=" + object,
                            node.getStartMark(), e.getMessage(), valueNode.getStartMark(), e);
                }
            }
            return object;
        }

        private Object newInstance(TypeDescription memberDescription, String propertyName,
                Node node) {
            Object newInstance = memberDescription.newInstance(propertyName, node);
            if (newInstance != null) {
                constructedObjects.put(node, newInstance);
                return constructObjectNoCheck(node);
            }
            return constructObject(node);
        }

        protected Property getProperty(Class<? extends Object> type, String name) {
            return getPropertyUtils().getProperty(type, name);
        }
    }

    /**
     * Construct an instance when the runtime class is not known but a global
     * tag with a class name is defined. It delegates the construction to the
     * appropriate constructor based on the node kind (scalar, sequence,
     * mapping)
     */
    protected class ConstructYamlObject implements Construct {

        private Construct getConstructor(Node node) {
            Class<?> cl = getClassForNode(node);
            node.setType(cl);
            // call the constructor as if the runtime class is defined
            Construct constructor = yamlClassConstructors.get(node.getNodeId());
            return constructor;
        }

        public Object construct(Node node) {
            try {
                return getConstructor(node).construct(node);
            } catch (ConstructorException e) {
                throw e;
            } catch (Exception e) {
                throw new ConstructorException(null, null, "Can't construct a java object for "
                        + node.getTag() + "; exception=" + e.getMessage(), node.getStartMark(), e);
            }
        }

        public void construct2ndStep(Node node, Object object) {
            try {
                getConstructor(node).construct2ndStep(node, object);
            } catch (Exception e) {
                throw new ConstructorException(
                        null, null, "Can't construct a second step for a java object for "
                                + node.getTag() + "; exception=" + e.getMessage(),
                        node.getStartMark(), e);
            }
        }
    }

    /**
     * Construct scalar instance when the runtime class is known. Recursive
     * structures are not supported.
     */
    protected class ConstructScalar extends AbstractConstruct {
        public Object construct(Node nnode) {
            ScalarNode node = (ScalarNode) nnode;
            Class<?> type = node.getType();

            try {
                return newInstance(type, node, false);
            } catch (InstantiationException e1) {
            }

            Object result;
            if (type.isPrimitive() || type == String.class || Number.class.isAssignableFrom(type)
                    || type == Boolean.class || Date.class.isAssignableFrom(type)
                    || type == Character.class || type == BigInteger.class
                    || type == BigDecimal.class || Enum.class.isAssignableFrom(type)
                    || Tag.BINARY.equals(node.getTag()) || Calendar.class.isAssignableFrom(type)
                    || type == UUID.class) {
                // standard classes created directly
                result = constructStandardJavaInstance(type, node);
            } else {
                // there must be only 1 constructor with 1 argument
                java.lang.reflect.Constructor<?>[] javaConstructors = type
                        .getDeclaredConstructors();
                int oneArgCount = 0;
                java.lang.reflect.Constructor<?> javaConstructor = null;
                for (java.lang.reflect.Constructor<?> c : javaConstructors) {
                    if (c.getParameterTypes().length == 1) {
                        oneArgCount++;
                        javaConstructor = c;
                    }
                }
                Object argument;
                if (javaConstructor == null) {
                    try {
                        return newInstance(type, node, false);
                    } catch (InstantiationException ie) {
                        throw new YAMLException("No single argument constructor found for " + type
                                + " : " + ie.getMessage());
                    }
                } else if (oneArgCount == 1) {
                    argument = constructStandardJavaInstance(javaConstructor.getParameterTypes()[0],
                            node);
                } else {
                    // TODO it should be possible to use implicit types instead
                    // of forcing String. Resolver must be available here to
                    // obtain the implicit tag. Then we can set the tag and call
                    // callConstructor(node) to create the argument instance.
                    // On the other hand it may be safer to require a custom
                    // constructor to avoid guessing the argument class
                    argument = constructScalar(node);
                    try {
                        javaConstructor = type.getDeclaredConstructor(String.class);
                    } catch (Exception e) {
                        throw new YAMLException("Can't construct a java object for scalar "
                                + node.getTag() + "; No String constructor found. Exception="
                                + e.getMessage(), e);
                    }
                }
                try {
                    javaConstructor.setAccessible(true);
                    result = javaConstructor.newInstance(argument);
                } catch (Exception e) {
                    throw new ConstructorException(null, null,
                            "Can't construct a java object for scalar " + node.getTag()
                                    + "; exception=" + e.getMessage(),
                            node.getStartMark(), e);
                }
            }
            return result;
        }

        @SuppressWarnings("unchecked")
        private Object constructStandardJavaInstance(@SuppressWarnings("rawtypes") Class type,
                ScalarNode node) {
            Object result;
            if (type == String.class) {
                Construct stringConstructor = yamlConstructors.get(Tag.STR);
                result = stringConstructor.construct(node);
            } else if (type == Boolean.class || type == Boolean.TYPE) {
                Construct boolConstructor = yamlConstructors.get(Tag.BOOL);
                result = boolConstructor.construct(node);
            } else if (type == Character.class || type == Character.TYPE) {
                Construct charConstructor = yamlConstructors.get(Tag.STR);
                String ch = (String) charConstructor.construct(node);
                if (ch.length() == 0) {
                    result = null;
                } else if (ch.length() != 1) {
                    throw new YAMLException(
                            "Invalid node Character: '" + ch + "'; length: " + ch.length());
                } else {
                    result = Character.valueOf(ch.charAt(0));
                }
            } else if (Date.class.isAssignableFrom(type)) {
                Construct dateConstructor = yamlConstructors.get(Tag.TIMESTAMP);
                Date date = (Date) dateConstructor.construct(node);
                if (type == Date.class) {
                    result = date;
                } else {
                    try {
                        java.lang.reflect.Constructor<?> constr = type.getConstructor(long.class);
                        result = constr.newInstance(date.getTime());
                    } catch (RuntimeException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new YAMLException("Cannot construct: '" + type + "'");
                    }
                }
            } else if (type == Float.class || type == Double.class || type == Float.TYPE
                    || type == Double.TYPE || type == BigDecimal.class) {
                if (type == BigDecimal.class) {
                    result = new BigDecimal(node.getValue());
                } else {
                    Construct doubleConstructor = yamlConstructors.get(Tag.FLOAT);
                    result = doubleConstructor.construct(node);
                    if (type == Float.class || type == Float.TYPE) {
                        result = Float.valueOf(((Double) result).floatValue());
                    }
                }
            } else if (type == Byte.class || type == Short.class || type == Integer.class
                    || type == Long.class || type == BigInteger.class || type == Byte.TYPE
                    || type == Short.TYPE || type == Integer.TYPE || type == Long.TYPE) {
                Construct intConstructor = yamlConstructors.get(Tag.INT);
                result = intConstructor.construct(node);
                if (type == Byte.class || type == Byte.TYPE) {
                    result = Byte.valueOf(result.toString());
                } else if (type == Short.class || type == Short.TYPE) {
                    result = Short.valueOf(result.toString());
                } else if (type == Integer.class || type == Integer.TYPE) {
                    result = Integer.parseInt(result.toString());
                } else if (type == Long.class || type == Long.TYPE) {
                    result = Long.valueOf(result.toString());
                } else {
                    // only BigInteger left
                    result = new BigInteger(result.toString());
                }
            } else if (Enum.class.isAssignableFrom(type)) {
                String enumValueName = node.getValue();
                try {
                    result = Enum.valueOf(type, enumValueName);
                } catch (Exception ex) {
                    throw new YAMLException("Unable to find enum value '" + enumValueName
                            + "' for enum class: " + type.getName());
                }
            } else if (Calendar.class.isAssignableFrom(type)) {
                ConstructYamlTimestamp contr = new ConstructYamlTimestamp();
                contr.construct(node);
                result = contr.getCalendar();
            } else if (Number.class.isAssignableFrom(type)) {
                //since we do not know the exact type we create Float
                ConstructYamlFloat contr = new ConstructYamlFloat();
                result = contr.construct(node);
            } else if (UUID.class == type) {
                result = UUID.fromString(node.getValue());
            } else {
                if (yamlConstructors.containsKey(node.getTag())) {
                    result = yamlConstructors.get(node.getTag()).construct(node);
                } else {
                    throw new YAMLException("Unsupported class: " + type);
                }
            }
            return result;
        }
    }

    /**
     * Construct sequence (List, Array, or immutable object) when the runtime
     * class is known.
     */
    protected class ConstructSequence implements Construct {
        @SuppressWarnings("unchecked")
        public Object construct(Node node) {
            SequenceNode snode = (SequenceNode) node;
            if (Set.class.isAssignableFrom(node.getType())) {
                if (node.isTwoStepsConstruction()) {
                    throw new YAMLException("Set cannot be recursive.");
                } else {
                    return constructSet(snode);
                }
            } else if (Collection.class.isAssignableFrom(node.getType())) {
                if (node.isTwoStepsConstruction()) {
                    return newList(snode);
                } else {
                    return constructSequence(snode);
                }
            } else if (node.getType().isArray()) {
                if (node.isTwoStepsConstruction()) {
                    return createArray(node.getType(), snode.getValue().size());
                } else {
                    return constructArray(snode);
                }
            } else {
                // create immutable object
                List<java.lang.reflect.Constructor<?>> possibleConstructors = new ArrayList<java.lang.reflect.Constructor<?>>(
                        snode.getValue().size());
                for (java.lang.reflect.Constructor<?> constructor : node.getType()
                        .getDeclaredConstructors()) {
                    if (snode.getValue().size() == constructor.getParameterTypes().length) {
                        possibleConstructors.add(constructor);
                    }
                }
                if (!possibleConstructors.isEmpty()) {
                    if (possibleConstructors.size() == 1) {
                        Object[] argumentList = new Object[snode.getValue().size()];
                        java.lang.reflect.Constructor<?> c = possibleConstructors.get(0);
                        int index = 0;
                        for (Node argumentNode : snode.getValue()) {
                            Class<?> type = c.getParameterTypes()[index];
                            // set runtime classes for arguments
                            argumentNode.setType(type);
                            argumentList[index++] = constructObject(argumentNode);
                        }

                        try {
                            c.setAccessible(true);
                            return c.newInstance(argumentList);
                        } catch (Exception e) {
                            throw new YAMLException(e);
                        }
                    }

                    // use BaseConstructor
                    List<Object> argumentList = (List<Object>) constructSequence(snode);
                    Class<?>[] parameterTypes = new Class[argumentList.size()];
                    int index = 0;
                    for (Object parameter : argumentList) {
                        parameterTypes[index] = parameter.getClass();
                        index++;
                    }

                    for (java.lang.reflect.Constructor<?> c : possibleConstructors) {
                        Class<?>[] argTypes = c.getParameterTypes();
                        boolean foundConstructor = true;
                        for (int i = 0; i < argTypes.length; i++) {
                            if (!wrapIfPrimitive(argTypes[i]).isAssignableFrom(parameterTypes[i])) {
                                foundConstructor = false;
                                break;
                            }
                        }
                        if (foundConstructor) {
                            try {
                                c.setAccessible(true);
                                return c.newInstance(argumentList.toArray());
                            } catch (Exception e) {
                                throw new YAMLException(e);
                            }
                        }
                    }
                }
                throw new YAMLException(
                        "No suitable constructor with " + String.valueOf(snode.getValue().size())
                                + " arguments found for " + node.getType());

            }
        }

        private final Class<? extends Object> wrapIfPrimitive(Class<?> clazz) {
            if (!clazz.isPrimitive()) {
                return clazz;
            }
            if (clazz == Integer.TYPE) {
                return Integer.class;
            }
            if (clazz == Float.TYPE) {
                return Float.class;
            }
            if (clazz == Double.TYPE) {
                return Double.class;
            }
            if (clazz == Boolean.TYPE) {
                return Boolean.class;
            }
            if (clazz == Long.TYPE) {
                return Long.class;
            }
            if (clazz == Character.TYPE) {
                return Character.class;
            }
            if (clazz == Short.TYPE) {
                return Short.class;
            }
            if (clazz == Byte.TYPE) {
                return Byte.class;
            }
            throw new YAMLException("Unexpected primitive " + clazz);
        }

        @SuppressWarnings("unchecked")
        public void construct2ndStep(Node node, Object object) {
            SequenceNode snode = (SequenceNode) node;
            if (List.class.isAssignableFrom(node.getType())) {
                List<Object> list = (List<Object>) object;
                constructSequenceStep2(snode, list);
            } else if (node.getType().isArray()) {
                constructArrayStep2(snode, object);
            } else {
                throw new YAMLException("Immutable objects cannot be recursive.");
            }
        }
    }

    protected Class<?> getClassForNode(Node node) {
        Class<? extends Object> classForTag = typeTags.get(node.getTag());
        if (classForTag == null) {
            String name = node.getTag().getClassName();
            Class<?> cl;
            try {
                cl = getClassForName(name);
            } catch (ClassNotFoundException e) {
                throw new YAMLException("Class not found: " + name);
            }
            typeTags.put(node.getTag(), cl);
            return cl;
        } else {
            return classForTag;
        }
    }

    protected Class<?> getClassForName(String name) throws ClassNotFoundException {
        try {
            return Class.forName(name, true, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            return Class.forName(name);
        }
    }
}
