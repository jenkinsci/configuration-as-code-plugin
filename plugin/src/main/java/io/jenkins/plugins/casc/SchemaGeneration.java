package io.jenkins.plugins.casc;

import java.util.*;
import io.jenkins.plugins.casc.impl.configurators.HeteroDescribableConfigurator;
import org.json.JSONException;
import org.json.JSONObject;

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
        schemaString.append("},\n");


        /**
         * This generates the schema for the base configurators
         * Iterates over the base configurators and adds them to the schema.
         */

        String output = "{";
        String output1="";
        ConfigurationAsCode configurationAsCodeObject = ConfigurationAsCode.get();
          for (Object configuratorObject : configurationAsCodeObject.getConfigurators()) {
                if (configuratorObject instanceof BaseConfigurator) {
                    BaseConfigurator baseConfigurator = (BaseConfigurator) configuratorObject;
                    List<Attribute> baseConfigAttributeList = baseConfigurator.getAttributes();
                    if(baseConfigAttributeList.size() == 0 ) {
                        output += "\"" + ((BaseConfigurator) configuratorObject).getTarget().toString() +
                        "\":{" + "\"type\": \"object\"," + "\"properties\": {}},";
                    } else {
                        for (Attribute attribute:baseConfigAttributeList) {
                            if(attribute.multiple) {
//                                System.out.println("Attribute type is multiple");
                            } else {
                                if(attribute.type.isEnum()) {
                                    if(attribute.type.getEnumConstants().length == 0){
                                        output += "\"" + attribute.getName() + "\":" + " {" +
                                                "\"type\": \"string\"}";
                                    }
                                    else {
                                        output += "\"" + attribute.getName() + "\":" + " {" +
                                                "\"type\": \"string\"," + "\"enum\": [";
                                        for (Object obj : attribute.type.getEnumConstants()) {
                                            output += "\"" + obj + "\",";
                                        }
                                        output += "]},";
                                    }
                                } else {
                                    output += "\"" + attribute.getName() + "\":";
                                    switch (attribute.type.getName()){

                                        case "java.lang.String":
                                            output += "{\"type\": \"string\"},";
                                            break;

                                        case "int":
                                            output += "{\"type\": \"integer\"},";
                                            break;

                                        case "boolean":
                                            output += "{\"type\": \"boolean\"},";
                                            break;

                                        case "java.lang.Boolean":
                                            output += "{\"type\": \"boolean\"},";
                                            break;

                                        case "java.lang.Integer":
                                            output += "{\"type\": \"integer\"},";
                                            break;

                                        case "java.lang.Long":
                                            output += "{\"type\": \"integer\"},";
                                            break;

                                        default:
                                            output += "{\"type\":  \"object\"," +
                                                    "\"$ref\": \"#/definitions/" + attribute.type.getName() + "\"},";
                                            break;
                                    }

                                }
                            }
                        }
                    }
                }
          }
        output += "}";
        System.out.println("Look at the beautiful Json data");
        System.out.println(output1);
        try {
            String indented = (new JSONObject(output)).toString(4);
            System.out.println(indented);
        } catch (JSONException e) {
            e.printStackTrace();
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

        schemaString.append("}");
        return schemaString.toString();
    }
}
