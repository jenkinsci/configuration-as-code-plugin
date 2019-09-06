package io.jenkins.plugins.casc;

import hudson.DescriptorExtensionList;
import hudson.model.Descriptor;
import io.jenkins.plugins.casc.impl.DefaultConfiguratorRegistry;
import io.jenkins.plugins.casc.impl.configurators.HeteroDescribableConfigurator;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jenkins.model.Jenkins;
import org.json.JSONArray;
import org.json.JSONObject;

public class SchemaGeneration {

    final static JSONObject schemaTemplateObject = new JSONObject()
        .put("$schema", "http://json-schema.org/draft-07/schema#")
        .put("id", "http://jenkins.io/configuration-as-code#")
        .put("description", "Jenkins Configuration as Code")
        .put("type", "object");

    public static JSONObject generateSchema() throws Exception {

        /**
         * The initial template for the JSON Schema
         */
        JSONObject schemaObject = new JSONObject(schemaTemplateObject.toString());
        /**
         * This generates the schema for the root configurators
         * Iterates over the root elements and adds them to the schema.
         */
        schemaObject.put("properties", generateRootConfiguratorObject());
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
//
//                /*Get rid of this*/
//                DefaultConfiguratorRegistry registry = new DefaultConfiguratorRegistry();
//                final ConfigurationContext context = new ConfigurationContext(registry);
//                List<Configurator> configurators = new ArrayList<>(baseConfigurator.getConfigurators(context)) ;
//                for(Configurator configurator : configurators) {
//                    System.out.println("BaseConfig is " + configurator.getName());
//                }
//                /*Get rid of this*/

                System.out.println("Base Configurator: " + baseConfigurator.getName());


                if (baseConfigAttributeList.size() == 0) {
                    schemaConfiguratorObjects
                        .put(((BaseConfigurator) configuratorObject).getTarget().getSimpleName().toLowerCase(),
                            new JSONObject()
                                .put("type", "object")
                                .put("properties", new JSONObject()));

                } else {
                    JSONObject attributeSchema = new JSONObject();
                    for (Attribute attribute : baseConfigAttributeList) {
                        System.out.println("       Attribute : " + attribute.getName().toLowerCase() + "Attr Type: "  + attribute.getType().getSimpleName());



                        if (attribute.multiple) {
                            generateMultipleAttributeSchema(attributeSchema, attribute);
                        } else {
                            if (attribute.type.isEnum()) {
                                generateEnumAttributeSchema(attributeSchema, attribute);
                            } else {
                                attributeSchema.put(attribute.getName(), generateNonEnumAttributeObject(attribute));
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

                /*Get rid of this*/
                System.out.println("HeteroDescribableConfigurator: " + heteroDescribableConfigurator.getTarget().getSimpleName().toLowerCase());

                DefaultConfiguratorRegistry registry = new DefaultConfiguratorRegistry();
                final ConfigurationContext context = new ConfigurationContext(registry);
                List<Configurator> configurators = new ArrayList<>(heteroDescribableConfigurator.getConfigurators(context)) ;

                for(Configurator configurator : configurators) {
                    System.out.println("     Descriptor is " + configurator.getName());
                }
                /*Get rid of this*/


                schemaConfiguratorObjects.put(heteroDescribableConfigurator.getTarget().getSimpleName().toLowerCase(),
                    generateHetroDescribableConfigObject(heteroDescribableConfigurator));
                }
            }
        schemaObject.put("properties", schemaConfiguratorObjects);
        return schemaObject;
    }

    private static JSONObject generateHetroDescribableConfigObject(HeteroDescribableConfigurator heteroDescribableConfiguratorObject)
        throws Exception {
        Map<String, Class> implementorsMap = heteroDescribableConfiguratorObject
            .getImplementors();
        JSONObject finalHetroConfiguratorObject = new JSONObject();
        if (implementorsMap.size() != 0) {
            Iterator<Map.Entry<String, Class>> itr = implementorsMap.entrySet().iterator();

            JSONArray oneOfJsonArray = new JSONArray();
            while (itr.hasNext()) {


                Map.Entry<String, Class> entry = itr.next();
                JSONObject implementorObject = new JSONObject();
                System.out.println("    SubHetro :"  + entry.getValue().getName());
                implementorObject.put("properties",
                    new JSONObject().put(entry.getKey(), new JSONObject()
                        .put("$id", "#/definitions/" + entry.getValue().getName())));
                oneOfJsonArray.put(implementorObject);
            }

            finalHetroConfiguratorObject.put("type", "object");
            finalHetroConfiguratorObject.put("oneOf", oneOfJsonArray);
        }

            return finalHetroConfiguratorObject;

    }

    private static JSONObject generateNonEnumAttributeObject(Attribute attribute) {
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
                attributeType.put("$id",
                    "#/definitions/" + attribute.type.getName());
                break;
        }
        return attributeType;
    }

    private static JSONObject generateRootConfiguratorObject() {
        JSONObject rootConfiguratorObject = new JSONObject();
        LinkedHashSet linkedHashSet = new LinkedHashSet<>(
            ConfigurationAsCode.get().getRootConfigurators());
        Iterator<RootElementConfigurator> i = linkedHashSet.iterator();
        while (i.hasNext()) {
            RootElementConfigurator rootElementConfigurator = i.next();
            rootConfiguratorObject
                .put(rootElementConfigurator.getName(), new JSONObject().put("type", "object"));
        }
        return rootConfiguratorObject;
    }

    private static void generateMultipleAttributeSchema(JSONObject attributeSchema, Attribute attribute) {

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

    private static void generateEnumAttributeSchema(JSONObject attributeSchemaTemplate, Attribute attribute) {
        if (attribute.type.getEnumConstants().length == 0) {
            System.out.println("     Sub-Enum Attribute : " + attribute.getName().toLowerCase() );
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

    public static void rootConfigGeneration() throws Exception {

        DefaultConfiguratorRegistry registry = new DefaultConfiguratorRegistry();
        final ConfigurationContext context = new ConfigurationContext(registry);
        context.setMode("JSONSchema");
        for (RootElementConfigurator root : RootElementConfigurator.all()) {
            final CNode config = root.describeStructure(root.getTargetComponent(context), context);
            final Mapping mapping = config.asMapping();
            final List<Map.Entry<String, CNode>> entries = new ArrayList<>(mapping.entrySet());
            for (Map.Entry<String, CNode> entry : entries) {
                System.out.println(entry.getKey() + " " + entry.getValue().toString());
            }
        }
    }


}

