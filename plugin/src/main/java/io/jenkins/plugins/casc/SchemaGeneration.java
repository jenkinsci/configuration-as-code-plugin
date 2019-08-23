package io.jenkins.plugins.casc;

import java.util.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.jenkins.plugins.casc.impl.configurators.HeteroDescribableConfigurator;

public class SchemaGeneration {

    public static String generateSchema() {

        /**
         * The initial template for the JSON Schema
         */
        StringBuilder schemaString = new StringBuilder();
        schemaString.append("{\n" +
                "  \"$schema\": \"http://json-schema.org/draft-06/schema#\",\n" +
                "  \"id\": \"http://jenkins.io/configuration-as-code#\",\n" +
                "  \"description\": \"Jenkins Configuration as Code\",\n" +
                "  \"type\": \"object\",\n" +
                "  \"properties\": {\n" +
                "");

        /**
         * This generates the schema for the root configurators
         * Iterates over the root elements and adds them to the schema.
         */
        LinkedHashSet linkedHashSet = new LinkedHashSet<>(ConfigurationAsCode.get().getRootConfigurators());
        Iterator<RootElementConfigurator> i = linkedHashSet.iterator();
        while (i.hasNext()) {
            RootElementConfigurator rootElementConfigurator = i.next();
            schemaString.append("\"" + rootElementConfigurator.getName() + "\": {" + "\"type\": \"object\",\n" +
                    "    },");
        }
        schemaString.append("},\n\"definitions\": {");


        /**
         * This generates the schema for the base configurators
         * Iterates over the base configurators and adds them to the schema.
         */

        ConfigurationAsCode configurationAsCodeObject = ConfigurationAsCode.get();
          for (Object configuratorObject : configurationAsCodeObject.getConfigurators()) {
                if (configuratorObject instanceof BaseConfigurator) {
                    BaseConfigurator baseConfigurator = (BaseConfigurator) configuratorObject;
                    List<Attribute> baseConfigAttributeList = baseConfigurator.getAttributes();
                    if(baseConfigAttributeList.size() == 0 ) {
                         schemaString.append("\"" + ((BaseConfigurator) configuratorObject).getTarget().toString() +
                        "\":{" + "\"type\": \"object\"," + "\"properties\": {}},");
                    } else {
                        for (Attribute attribute:baseConfigAttributeList) {
                            if(attribute.multiple) {
                                schemaString.append("\"" + attribute.getName() + "\":");

                                if(attribute.type.getName().equals("java.lang.String")) {
                                    schemaString.append("{\"type\": \"string\"},");
                                } else {
                                     schemaString.append("{\"type\":  \"object\"," +
                                            "\"$ref\": \"#/definitions/" + attribute.type.getName() + "\"},");
                                }
                            } else {
                                if(attribute.type.isEnum()) {
                                    if(attribute.type.getEnumConstants().length == 0){
                                        schemaString.append("\"" + attribute.getName() + "\":" + " {" +
                                                "\"type\": \"string\"}");
                                    }
                                    else {
                                        schemaString.append("\"" + attribute.getName() + "\":" + " {" +
                                                "\"type\": \"string\"," + "\"enum\": [");

                                        for (Object obj : attribute.type.getEnumConstants()) {
                                            schemaString.append("\"" + obj + "\",");
                                        }
                                         schemaString.append("]},");
                                    }
                                } else {
                                    schemaString.append("\"" + attribute.getName() + "\":");
                                    switch (attribute.type.getName()){

                                        case "java.lang.String":
                                            schemaString.append("{\n\"type\": \"string\"},\n");
                                            break;

                                        case "int":
                                            schemaString.append("{\n\"type\": \"integer\"},\n");
                                            break;

                                        case "boolean":
                                             schemaString.append("{\n\"type\": \"boolean\"},\n");
                                            break;

                                        case "java.lang.Boolean":
                                            schemaString.append("{\"type\": \"boolean\"},\n");
                                            break;

                                        case "java.lang.Integer":
                                            schemaString.append("{\n\"type\": \"integer\"},\n");
                                            break;

                                        case "java.lang.Long":
                                            schemaString.append("{\n\"type\": \"integer\"},\n");
                                            break;

                                        default:
                                            schemaString.append("{\n\"type\":  \"object\",\n" +
                                                    "\"$ref\": \"#/definitions/" + attribute.type.getName() + "\"},\n");
                                            break;
                                    }

                                }
                            }
                        }
                    }
                }
          }

        /**
         * Used to generate the schema for the implementors of
         * the HetroDescribable Configurator
         * It mimics the HetroDescribable Configurator
         */
        ConfigurationAsCode configurationAsCode = ConfigurationAsCode.get();
        configurationAsCode.getConfigurators()
                .stream()
                .forEach(configurator -> {
                    if(configurator instanceof HeteroDescribableConfigurator) {
                        HeteroDescribableConfigurator heteroDescribableConfigurator = (HeteroDescribableConfigurator) configurator;
                        Map<String,Class> implementorsMap = heteroDescribableConfigurator.getImplementors();

                        if(implementorsMap.size() != 0) {
                            schemaString.append("\"" + heteroDescribableConfigurator.getTarget().getName() + "\": {\n" +
                                    " \"type\": \"object\"\n" +
                                            "    ," + "\"oneOf\" : [");
                            Iterator<Map.Entry<String, Class>> itr = implementorsMap.entrySet().iterator();
                            while (itr.hasNext()) {
                                Map.Entry<String, Class> entry = itr.next();
                                schemaString.append("{\n" +
                                        "      \"properties\" : {\n" +
                                        "        \"" + entry.getKey() + "\" : { \"$ref\" : \"#/definitions/ " + entry.getValue() + "\" }\n" +
                                        "      }\n" +
                                        "    }\n" +
                                        "    ,");
                            }
                            schemaString.append("]\n" + "  \n" + "  }\n" + ",");
                        }
                    }
                });

        schemaString.append("}\n}");
        return schemaString.toString();
    }
}
