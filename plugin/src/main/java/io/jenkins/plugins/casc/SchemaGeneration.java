package io.jenkins.plugins.casc;

import hudson.model.Descriptor;
import io.jenkins.plugins.casc.impl.DefaultConfiguratorRegistry;
import io.jenkins.plugins.casc.impl.attributes.DescribableAttribute;
import io.jenkins.plugins.casc.impl.configurators.DescriptorConfigurator;
import io.jenkins.plugins.casc.impl.configurators.HeteroDescribableConfigurator;
import io.vavr.collection.Stream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

public class SchemaGeneration {

    final static JSONObject schemaTemplateObject = new JSONObject()
        .put("$schema", "http://json-schema.org/draft-07/schema#")
        .put("description", "Jenkins Configuration as Code")
        .put("additionalProperties", false)
        .put("type", "object");


    public static JSONObject generateSchema() {

        /**
         * The initial template for the JSON Schema
         */
        JSONObject schemaObject = new JSONObject(schemaTemplateObject.toString());
        /**
         * This generates the schema for the base configurators
         * Iterates over the base configurators and adds them to the schema.
         */
        DefaultConfiguratorRegistry registry = new DefaultConfiguratorRegistry();
        final ConfigurationContext context = new ConfigurationContext(registry);

        JSONObject rootConfiguratorProperties = new JSONObject();
        for (RootElementConfigurator rootElementConfigurator : RootElementConfigurator.all()) {
            JSONObject schemaConfiguratorObjects = new JSONObject();
            Set<Object> elements = new LinkedHashSet<>();
            listElements(elements, rootElementConfigurator.describe(), context, true);
            for (Object configuratorObject : elements) {
                if (configuratorObject instanceof BaseConfigurator) {
                    BaseConfigurator baseConfigurator = (BaseConfigurator) configuratorObject;
                    List<Attribute> baseConfigAttributeList = baseConfigurator.getAttributes();

                    if (baseConfigAttributeList.size() == 0) {
                        schemaConfiguratorObjects
                            .put(baseConfigurator.getName().toLowerCase(),
                                new JSONObject()
                                    .put("additionalProperties", false)
                                    .put("type", "object")
                                    .put("properties", new JSONObject()));

                    } else {
                        JSONObject attributeSchema = new JSONObject();
                        for (Attribute attribute : baseConfigAttributeList) {
                            if (attribute.multiple) {
                                generateMultipleAttributeSchema(attributeSchema, attribute);
                            } else {
                                if (attribute.type.isEnum()) {
                                    generateEnumAttributeSchema(attributeSchema, attribute);
                                } else {
                                    attributeSchema.put(attribute.getName(),
                                        generateNonEnumAttributeObject(attribute));
                                    String key;
                                    if (baseConfigurator instanceof DescriptorConfigurator) {
                                        key = ((DescriptorConfigurator) configuratorObject)
                                            .getName();
                                    } else {
                                        key = ((BaseConfigurator) configuratorObject).getTarget()
                                            .getSimpleName().toLowerCase();
                                    }
                                    schemaConfiguratorObjects
                                        .put(key,
                                            new JSONObject()
                                                .put("additionalProperties", false)
                                                .put("type", "object")
                                                .put("properties", attributeSchema));
                                }
                            }
                        }
                    }
                } else if (configuratorObject instanceof HeteroDescribableConfigurator) {
                    HeteroDescribableConfigurator heteroDescribableConfigurator = (HeteroDescribableConfigurator) configuratorObject;
                    Stream descriptors = heteroDescribableConfigurator.getDescriptors();
                    String key;
                    if (!descriptors.isEmpty()) {
                        key = DescribableAttribute.getPreferredSymbol(
                            (Descriptor) descriptors.get(0),
                            heteroDescribableConfigurator.getTarget(),
                            heteroDescribableConfigurator.getTarget());
                    } else {
                        key = heteroDescribableConfigurator.getTarget().getSimpleName()
                            .toLowerCase();
                    }
                    schemaConfiguratorObjects
                        .put(
                            key,
                            generateHeteroDescribableConfigObject(heteroDescribableConfigurator));
                } else if (configuratorObject instanceof Attribute) {
                    Attribute attribute = (Attribute) configuratorObject;
                    if (attribute.type.isEnum()) {
                        generateEnumAttributeSchema(schemaConfiguratorObjects, attribute);
                    } else {
                        schemaConfiguratorObjects
                            .put(attribute.getName(), generateNonEnumAttributeObject(attribute));
                    }

                }
            }

            rootConfiguratorProperties.put(rootElementConfigurator.getName(),
                new JSONObject().put("type", "object")
                    .put("additionalProperties", false)
                    .put("properties", schemaConfiguratorObjects)
                    .put("title", "Configuration base for the " + rootElementConfigurator.getName()
                        + " classifier"));
        }
        schemaObject.put("properties", rootConfiguratorProperties);
        return schemaObject;
    }

    public static String writeJSONSchema() {
        return generateSchema().toString(4);
    }

    private static JSONObject generateHeteroDescribableConfigObject(
        HeteroDescribableConfigurator heteroDescribableConfiguratorObject) {
        Map<String, Class> implementorsMap = heteroDescribableConfiguratorObject
            .getImplementors();
        JSONObject finalHeteroConfiguratorObject = new JSONObject();
        if (!implementorsMap.isEmpty()) {
            Iterator<Map.Entry<String, Class>> itr = implementorsMap.entrySet().iterator();

            JSONArray oneOfJsonArray = new JSONArray();
            while (itr.hasNext()) {
                Map.Entry<String, Class> entry = itr.next();
                JSONObject implementorObject = new JSONObject();
                implementorObject.put("additionalProperties", false);
                implementorObject.put("properties",
                    new JSONObject().put(entry.getKey(), new JSONObject()
                        .put("$id", "#/definitions/" + entry.getValue().getName())));
                oneOfJsonArray.put(implementorObject);
            }

            finalHeteroConfiguratorObject.put("type", "object");
            finalHeteroConfiguratorObject.put("oneOf", oneOfJsonArray);
        }
        return finalHeteroConfiguratorObject;
    }

    /**
     * Recursive configurators tree walk (DFS) and non-describable able attributes. Collects all
     * configurators starting from root ones in {@link ConfigurationAsCode#getConfigurators()}
     *
     * @param elements linked set (to save order) of visited elements
     * @param attributes siblings to find associated configurators and dive to next tree levels
     * @param context configuration context
     * @param root is this the first iteration of root attributes
     */
    private static void listElements(Set<Object> elements, Set<Attribute<?, ?>> attributes,
        ConfigurationContext context, boolean root) {
        // some unexpected type erasure force to cast here
        attributes.stream()
            .peek(attribute -> {
                // root primitive attributes are skipped without this
                if (root && !(attribute instanceof DescribableAttribute)) {
                    elements.add(attribute);
                }
            })
            .map(attribute -> attribute.getType())
            .map(type -> context.lookup(type))
            .filter(obj -> Objects.nonNull(obj))
            .map(c -> c.getConfigurators(context))
            .flatMap(configurators -> configurators.stream())
            .filter(e -> elements.add(e))
            .forEach(
                configurator -> listElements(elements, ((Configurator) configurator).describe(),
                    context, false)
            );
    }


    private static JSONObject generateNonEnumAttributeObject(Attribute attribute) {
        JSONObject attributeType = new JSONObject();
        switch (attribute.type.getName()) {
            case "java.lang.String":
            case "hudson.Secret":
                attributeType.put("type", "string");
                break;

            case "int":
            case "java.lang.Integer":
            case "java.lang.Long":
                attributeType.put("type", "integer");
                break;

            case "boolean":
            case "java.lang.Boolean":
                attributeType.put("type", "boolean");
                break;

            default:
                attributeType.put("type", "object");
                attributeType.put("additionalProperties", false);
                attributeType.put("$id",
                    "#/definitions/" + attribute.type.getName());
                break;
        }
        return attributeType;
    }

    private static void generateMultipleAttributeSchema(JSONObject attributeSchema,
        Attribute attribute) {
        if (attribute.type.getName().equals("java.lang.String")) {
            attributeSchema.put(attribute.getName(),
                new JSONObject()
                    .put("type", "string"));
        } else {
            attributeSchema.put(attribute.getName(),
                new JSONObject()
                    .put("type", "object")
                    .put("$id", "#/definitions/" + attribute.type.getName()));
        }
    }

    private static void generateEnumAttributeSchema(JSONObject attributeSchemaTemplate,
        Attribute attribute) {

        if (attribute.type.getEnumConstants().length == 0) {
            attributeSchemaTemplate.put(attribute.getName(),
                new JSONObject()
                    .put("type", "string"));
        } else {
            ArrayList<String> attributeList = new ArrayList<>();
            for (Object obj : attribute.type.getEnumConstants()) {
                attributeList.add(obj.toString());
            }
            attributeSchemaTemplate.put(attribute.getName(),
                new JSONObject()
                    .put("type", "string")
                    .put("enum", new JSONArray(attributeList)));
        }
    }
}
