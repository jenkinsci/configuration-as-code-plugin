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
package io.jenkins.plugins.casc.snakeyaml.extensions.compactnotation;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.jenkins.plugins.casc.snakeyaml.constructor.Construct;
import io.jenkins.plugins.casc.snakeyaml.constructor.Constructor;
import io.jenkins.plugins.casc.snakeyaml.error.YAMLException;
import io.jenkins.plugins.casc.snakeyaml.introspector.Property;
import io.jenkins.plugins.casc.snakeyaml.nodes.MappingNode;
import io.jenkins.plugins.casc.snakeyaml.nodes.Node;
import io.jenkins.plugins.casc.snakeyaml.nodes.NodeTuple;
import io.jenkins.plugins.casc.snakeyaml.nodes.ScalarNode;
import io.jenkins.plugins.casc.snakeyaml.nodes.SequenceNode;

/**
 * Construct a custom Java instance out of a compact object notation format.
 */
public class CompactConstructor extends Constructor {
    private static final Pattern GUESS_COMPACT = Pattern
            .compile("\\p{Alpha}.*\\s*\\((?:,?\\s*(?:(?:\\w*)|(?:\\p{Alpha}\\w*\\s*=.+))\\s*)+\\)");
    private static final Pattern FIRST_PATTERN = Pattern.compile("(\\p{Alpha}.*)(\\s*)\\((.*?)\\)");
    private static final Pattern PROPERTY_NAME_PATTERN = Pattern
            .compile("\\s*(\\p{Alpha}\\w*)\\s*=(.+)");
    private Construct compactConstruct;

    protected Object constructCompactFormat(ScalarNode node, CompactData data) {
        try {
            Object obj = createInstance(node, data);
            Map<String, Object> properties = new HashMap<String, Object>(data.getProperties());
            setProperties(obj, properties);
            return obj;
        } catch (Exception e) {
            throw new YAMLException(e);
        }
    }

    protected Object createInstance(ScalarNode node, CompactData data) throws Exception {
        Class<?> clazz = getClassForName(data.getPrefix());
        Class<?>[] args = new Class[data.getArguments().size()];
        for (int i = 0; i < args.length; i++) {
            // assume all the arguments are Strings
            args[i] = String.class;
        }
        java.lang.reflect.Constructor<?> c = clazz.getDeclaredConstructor(args);
        c.setAccessible(true);
        return c.newInstance(data.getArguments().toArray());

    }

    protected void setProperties(Object bean, Map<String, Object> data) throws Exception {
        if (data == null) {
            throw new NullPointerException("Data for Compact Object Notation cannot be null.");
        }
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Property property = getPropertyUtils().getProperty(bean.getClass(), key);
            try {
                property.set(bean, entry.getValue());
            } catch (IllegalArgumentException e) {
                throw new YAMLException("Cannot set property='" + key + "' with value='"
                        + data.get(key) + "' (" + data.get(key).getClass() + ") in " + bean);
            }
        }
    }

    public CompactData getCompactData(String scalar) {
        if (!scalar.endsWith(")")) {
            return null;
        }
        if (scalar.indexOf('(') < 0) {
            return null;
        }
        Matcher m = FIRST_PATTERN.matcher(scalar);
        if (m.matches()) {
            String tag = m.group(1).trim();
            String content = m.group(3);
            CompactData data = new CompactData(tag);
            if (content.length() == 0)
                return data;
            String[] names = content.split("\\s*,\\s*");
            for (int i = 0; i < names.length; i++) {
                String section = names[i];
                if (section.indexOf('=') < 0) {
                    data.getArguments().add(section);
                } else {
                    Matcher sm = PROPERTY_NAME_PATTERN.matcher(section);
                    if (sm.matches()) {
                        String name = sm.group(1);
                        String value = sm.group(2).trim();
                        data.getProperties().put(name, value);
                    } else {
                        return null;
                    }
                }
            }
            return data;
        }
        return null;
    }

    private Construct getCompactConstruct() {
        if (compactConstruct == null) {
            compactConstruct = createCompactConstruct();
        }
        return compactConstruct;
    }

    protected Construct createCompactConstruct() {
        return new ConstructCompactObject();
    }

    @Override
    protected Construct getConstructor(Node node) {
        if (node instanceof MappingNode) {
            MappingNode mnode = (MappingNode) node;
            List<NodeTuple> list = mnode.getValue();
            if (list.size() == 1) {
                NodeTuple tuple = list.get(0);
                Node key = tuple.getKeyNode();
                if (key instanceof ScalarNode) {
                    ScalarNode scalar = (ScalarNode) key;
                    if (GUESS_COMPACT.matcher(scalar.getValue()).matches()) {
                        return getCompactConstruct();
                    }
                }
            }
        } else if (node instanceof ScalarNode) {
            ScalarNode scalar = (ScalarNode) node;
            if (GUESS_COMPACT.matcher(scalar.getValue()).matches()) {
                return getCompactConstruct();
            }
        }
        return super.getConstructor(node);
    }

    public class ConstructCompactObject extends ConstructMapping {

        @Override
        public void construct2ndStep(Node node, Object object) {
            // Compact Object Notation may contain only one entry
            MappingNode mnode = (MappingNode) node;
            NodeTuple nodeTuple = mnode.getValue().iterator().next();

            Node valueNode = nodeTuple.getValueNode();

            if (valueNode instanceof MappingNode) {
                valueNode.setType(object.getClass());
                constructJavaBean2ndStep((MappingNode) valueNode, object);
            } else {
                // value is a list
                applySequence(object, constructSequence((SequenceNode) valueNode));
            }
        }

        /*
         * MappingNode and ScalarNode end up here only they assumed to be a
         * compact object's representation (@see getConstructor(Node) above)
         */
        public Object construct(Node node) {
            ScalarNode tmpNode;
            if (node instanceof MappingNode) {
                // Compact Object Notation may contain only one entry
                MappingNode mnode = (MappingNode) node;
                NodeTuple nodeTuple = mnode.getValue().iterator().next();
                node.setTwoStepsConstruction(true);
                tmpNode = (ScalarNode) nodeTuple.getKeyNode();
                // return constructScalar((ScalarNode) keyNode);
            } else {
                tmpNode = (ScalarNode) node;
            }

            CompactData data = getCompactData(tmpNode.getValue());
            if (data == null) { // TODO: Should we throw an exception here ?
                return constructScalar(tmpNode);
            }
            return constructCompactFormat(tmpNode, data);
        }
    }

    protected void applySequence(Object bean, List<?> value) {
        try {
            Property property = getPropertyUtils().getProperty(bean.getClass(),
                    getSequencePropertyName(bean.getClass()));
            property.set(bean, value);
        } catch (Exception e) {
            throw new YAMLException(e);
        }
    }

    /**
     * Provide the name of the property which is used when the entries form a
     * sequence. The property must be a List.
     * @param bean the class to provide exactly one List property
     * @return name of the List property
     */
    protected String getSequencePropertyName(Class<?> bean) {
        Set<Property> properties = getPropertyUtils().getProperties(bean);
        for (Iterator<Property> iterator = properties.iterator(); iterator.hasNext();) {
            Property property = iterator.next();
            if (!List.class.isAssignableFrom(property.getType())) {
                iterator.remove();
            }
        }
        if (properties.size() == 0) {
            throw new YAMLException("No list property found in " + bean);
        } else if (properties.size() > 1) {
            throw new YAMLException(
                    "Many list properties found in "
                            + bean
                            + "; Please override getSequencePropertyName() to specify which property to use.");
        }
        return properties.iterator().next().getName();
    }
}
