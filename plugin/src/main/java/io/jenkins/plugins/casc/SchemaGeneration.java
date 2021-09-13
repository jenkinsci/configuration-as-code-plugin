package io.jenkins.plugins.casc;

import io.jenkins.plugins.casc.impl.DefaultConfiguratorRegistry;
import io.jenkins.plugins.casc.impl.attributes.DescribableAttribute;
import io.jenkins.plugins.casc.impl.configurators.HeteroDescribableConfigurator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.Beta;

@Restricted(Beta.class)
public class SchemaGeneration {

    private static final Logger LOGGER = Logger.getLogger(SchemaGeneration.class.getName());

    final static JSONObject schemaTemplateObject = new JSONObject()
        .put("$schema", "http://json-schema.org/draft-07/schema#")
        .put("description", "Jenkins Configuration as Code")
        .put("additionalProperties", false)
        .put("type", "object");


    public static JSONObject generateSchema() {
        JSONObject schemaObject = new JSONObject(schemaTemplateObject.toString());
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
                        String key = baseConfigurator.getName();
                        schemaConfiguratorObjects
                            .put(key,
                                new JSONObject()
                                    .put("additionalProperties", false)
                                    .put("type", "object")
                                    .put("properties", new JSONObject()));

                    } else {
                        JSONObject attributeSchema = new JSONObject();
                        for (Attribute attribute : baseConfigAttributeList) {
                            if (attribute.multiple) {
                                generateMultipleAttributeSchema(attributeSchema, attribute, context, baseConfigurator);
                            } else {
                                if (attribute.type.isEnum()) {
                                    generateEnumAttributeSchema(attributeSchema, attribute, baseConfigurator);
                                } else {
                                    attributeSchema.put(attribute.getName(),
                                        generateNonEnumAttributeObject(attribute, baseConfigurator));
                                    String key = baseConfigurator.getName();

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
                    String key = heteroDescribableConfigurator.getName();
                    schemaConfiguratorObjects
                        .put(
                            key,
                            generateHeteroDescribableConfigObject(heteroDescribableConfigurator));
                } else if (configuratorObject instanceof Attribute) {
                    Attribute attribute = (Attribute) configuratorObject;
                    if (attribute.type.isEnum()) {
                        generateEnumAttributeSchema(schemaConfiguratorObjects, attribute, null);
                    } else {
                        schemaConfiguratorObjects
                            .put(attribute.getName(), generateNonEnumAttributeObject(attribute, null));
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
            .map(Attribute::getType)
            .map(context::lookup)
            .filter(Objects::nonNull)
            .map(c -> c.getConfigurators(context))
            .flatMap(Collection::stream)
            .filter(elements::add)
            .forEach(
                configurator -> listElements(elements, ((Configurator) configurator).describe(),
                    context, false)
            );
    }


    private static JSONObject generateNonEnumAttributeObject(Attribute attribute, BaseConfigurator baseConfigurator) {
        JSONObject attributeType = new JSONObject();
        Optional<String> description = getDescription(attribute, baseConfigurator);
        switch (attribute.type.getName()) {
            case "java.lang.String":
            case "hudson.Secret":
                attributeType.put("type", "string");
                description.ifPresent(desc -> attributeType.put("description", desc));
                break;

            case "int":
            case "java.lang.Integer":
            case "long":
            case "java.lang.Long":
                attributeType.put("type", "integer");
                description.ifPresent(desc -> attributeType.put("description", desc));
                break;

            case "boolean":
            case "java.lang.Boolean":
                attributeType.put("type", "boolean");
                description.ifPresent(desc -> attributeType.put("description", desc));
                break;

            default:
                attributeType.put("type", "object");
                description.ifPresent(desc -> attributeType.put("description", desc));
                attributeType.put("additionalProperties", false);
                attributeType.put("$id",
                    "#/definitions/" + attribute.type.getName());
                break;
        }
        return attributeType;
    }

    private static Optional<String> getDescription(Attribute attribute, BaseConfigurator baseConfigurator) {
        String description = null;
        if (baseConfigurator != null) {
            description = retrieveDocStringFromAttribute(baseConfigurator.getTarget(), attribute.name);
        }
        return Optional.ofNullable(description);
    }

    private static void generateMultipleAttributeSchema(
        JSONObject attributeSchema,
        Attribute attribute,
        ConfigurationContext context,
        BaseConfigurator baseConfigurator
    ) {
        Optional<String> description = getDescription(attribute, baseConfigurator);

        if (attribute.type.getName().equals("java.lang.String")) {
            JSONObject jsonObject = new JSONObject()
                .put("type", "string");
            description.ifPresent(desc -> jsonObject.put("description", desc));
            attributeSchema.put(attribute.getName(),
                jsonObject)
            ;

        } else {
            JSONObject properties = new JSONObject();
            Configurator<Object> lookup = context.lookup(attribute.getType());
            if (lookup != null) {
                lookup
                    .getAttributes()
                    .forEach(attr -> properties
                        .put(attr.getName(), generateNonEnumAttributeObject(attr, baseConfigurator)));
            }

            JSONObject attributeObject = new JSONObject()
                .put("type", "array")
                .put("items", new JSONArray()
                    .put(new JSONObject()
                        .put("type", "object")
                        .put("properties", properties)
                        .put("additionalProperties", false)
                    )
                );
            description.ifPresent(desc -> attributeObject.put("description", desc));
            attributeSchema.put(attribute.getName(),
                attributeObject
            );
        }
    }

    private static void generateEnumAttributeSchema(JSONObject attributeSchemaTemplate,
        Attribute attribute, BaseConfigurator baseConfigurator) {
        Optional<String> description = getDescription(attribute, baseConfigurator);


        if (attribute.type.getEnumConstants().length == 0) {
            JSONObject jsonObject = new JSONObject()
                .put("type", "string");
            description.ifPresent(desc -> jsonObject.put("description", desc));
            attributeSchemaTemplate.put(attribute.getName(),
                jsonObject
            );
        } else {
            ArrayList<String> attributeList = new ArrayList<>();
            for (Object obj : attribute.type.getEnumConstants()) {
                attributeList.add(obj.toString());
            }
            JSONObject jsonObject = new JSONObject()
                .put("type", "string")
                .put("enum", new JSONArray(attributeList));
            description.ifPresent(desc -> jsonObject.put("description", desc));
            attributeSchemaTemplate.put(attribute.getName(), jsonObject);
        }
    }

    public static String retrieveDocStringFromAttribute(Class baseConfigClass, String attributeName) {
        try {
            String htmlDocString = ConfigurationAsCode.get().getHtmlHelp(baseConfigClass, attributeName);
            return removeHtmlTags(htmlDocString);
        } catch (IOException e) {
            LOGGER.warning("Error getting help document for attribute : " + e);
        }
        return null;
    }

    public static String removeHtmlTags(String htmlDocString) {
        return htmlDocString.replaceAll("<.*?>", "").trim();
    }
}
