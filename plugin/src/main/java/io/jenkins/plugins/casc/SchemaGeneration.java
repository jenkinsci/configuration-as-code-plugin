package io.jenkins.plugins.casc;

import io.jenkins.plugins.casc.impl.configurators.HeteroDescribableConfigurator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

public class SchemaGeneration {


    final static JSONObject schemaTemplateObject = new JSONObject()
        .put("$schema", "http://json-schema.org/draft-06/schema#")
        .put("id", "http://jenkins.io/configuration-as-code#")
        .put("description", "Jenkins Configuration as Code")
        .put("type", "object");

    public static JSONObject generateSchema() {

        /**
         * The initial template for the JSON Schema
         */

        JSONObject schemaObject = new JSONObject(schemaTemplateObject.toString());

        /**
         * This generates the schema for the root configurators
         * Iterates over the root elements and adds them to the schema.
         */

        JSONObject rootConfiguratorObject = new JSONObject();
        LinkedHashSet linkedHashSet = new LinkedHashSet<>(
            ConfigurationAsCode.get().getRootConfigurators());
        Iterator<RootElementConfigurator> i = linkedHashSet.iterator();
        while (i.hasNext()) {
            RootElementConfigurator rootElementConfigurator = i.next();
            rootConfiguratorObject
                .put(rootElementConfigurator.getName(), new JSONObject().put("type", "object"));
        }
        schemaObject.put("properties", rootConfiguratorObject);


        /**
         * This generates the schema for the base configurators
         * Iterates over the base configurators and adds them to the schema.
         */

        JSONObject schemaConfiguratorObjects = new JSONObject();
        ConfigurationAsCode configurationAsCodeObject = ConfigurationAsCode.get();
        for (Object configuratorObject : configurationAsCodeObject.getConfigurators()) {
            if (configuratorObject instanceof BaseConfigurator) {
                BaseConfigurator baseConfigurator = (BaseConfigurator) configuratorObject;
                List<Attribute> baseConfigAttributeList = baseConfigurator.getAttributes();
                if (baseConfigAttributeList.size() == 0) {


                    schemaConfiguratorObjects
                        .put(((BaseConfigurator) configuratorObject).getTarget().getSimpleName().toLowerCase(),
                            new JSONObject()
                                .put("type", "object")
                                .put("properties", new JSONObject()));

                } else {


                    JSONObject attributeSchema = new JSONObject();
                    for (Attribute attribute : baseConfigAttributeList) {
                        if (attribute.multiple) {

                            if (attribute.type.getName().equals("java.lang.String")) {
                                attributeSchema.put(attribute.getName(),
                                    new JSONObject()
                                        .put("type", "string"));
                            } else {
                                attributeSchema.put(attribute.getName(),
                                    new JSONObject()
                                        .put("type", "object")
                                        .put("$ref", "#/definitions/" + attribute.type.getName()));
                            }
                        } else {
                            if (attribute.type.isEnum()) {
                                if (attribute.type.getEnumConstants().length == 0) {
                                    attributeSchema.put(attribute.getName(),
                                        new JSONObject()
                                            .put("type", "string"));
                                } else {

                                    ArrayList<String> attributeList = new ArrayList<>();
                                    for (Object obj : attribute.type.getEnumConstants()) {
                                        attributeList.add(obj.toString());
                                    }
                                    attributeSchema.put(attribute.getName(),
                                        new JSONObject()
                                            .put("type", "string")
                                            .put("enum", new JSONArray(attributeList)));
                                }
                            } else {
                                JSONObject attributeType = new JSONObject();
                                switch (attribute.type.getName()) {

                                    case "java.lang.String":
                                        attributeType.put("type", "string");
                                        break;

                                    case "int":
                                        attributeType.put("type", "integer");
                                        break;

                                    case "boolean":
                                        attributeType.put("type", "boolean");
                                        break;

                                    case "java.lang.Boolean":
                                        attributeType.put("type", "boolean");
                                        break;

                                    case "java.lang.Integer":
                                        attributeType.put("type", "integer");
                                        break;

                                    case "java.lang.Long":
                                        attributeType.put("type", "integer");
                                        break;

                                    default:
                                        attributeType.put("type", "object");
                                        attributeType.put("$ref",
                                            "#/definitions/" + attribute.type.getName());
                                        break;
                                }

                                attributeSchema.put(attribute.getName(), attributeType);
                                schemaConfiguratorObjects
                                    .put(((BaseConfigurator) configuratorObject).getTarget().getSimpleName().toLowerCase(),
                                        new JSONObject()
                                            .put("type", "object")
                                            .put("properties", attributeSchema));
                            }
                        }
                    }
                }
            }

            /**
             * Used to generate the schema for the implementors of
             * the HetroDescribable Configurator
             * It mimics the HetroDescribable Configurator.jelly
             */

            else if (configuratorObject instanceof HeteroDescribableConfigurator) {
                HeteroDescribableConfigurator heteroDescribableConfigurator = (HeteroDescribableConfigurator) configuratorObject;
                Map<String, Class> implementorsMap = heteroDescribableConfigurator
                    .getImplementors();
                if (implementorsMap.size() != 0) {
                    Iterator<Map.Entry<String, Class>> itr = implementorsMap.entrySet().iterator();

                    JSONArray oneOfJsonArray = new JSONArray();
                    JSONObject finalHetroConfiguratorObject = new JSONObject();
                    while (itr.hasNext()) {
                        Map.Entry<String, Class> entry = itr.next();
                        JSONObject implementorObject = new JSONObject();
                        implementorObject.put("properties",
                            new JSONObject().put(entry.getKey(), new JSONObject().put("$ref","#/definitions/" + entry.getValue().getName())));
                        oneOfJsonArray.put(implementorObject);
                    }

                    finalHetroConfiguratorObject.put("type","object");
                    finalHetroConfiguratorObject.put("oneOf",oneOfJsonArray);

                    schemaConfiguratorObjects.put(heteroDescribableConfigurator.getTarget().getSimpleName().toLowerCase(), finalHetroConfiguratorObject);
                }
            }
        }

        schemaObject.put("definitions", schemaConfiguratorObjects);
        return schemaObject;
    }
}

