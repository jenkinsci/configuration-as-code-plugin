package io.jenkins.plugins.casc;

import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.DescriptorExtensionList;

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
        ConfigurationAsCode configurationAsCode1 = ConfigurationAsCode.get();
          for (Object configurator1 : configurationAsCode1.getConfigurators()) {
                if (configurator1 instanceof BaseConfigurator) {
                    BaseConfigurator baseConfigurator = (BaseConfigurator) configurator1;
                    if(baseConfigurator.getAttributes().size() ==0 ) {
                        output += "\"" + ((BaseConfigurator) configurator1).getTarget().toString() +
                        "\":{" + "\"type\": \"object\"," + "\"properties\": {}},";
                    }
                }
          }
        output += "}";
        System.out.println("Look at the beautiful Json data");
        System.out.println(output);
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
