package io.jenkins.plugins.casc;

import hudson.DescriptorExtensionList;

import java.util.*;

import io.jenkins.plugins.casc.impl.configurators.HeteroDescribableConfigurator;
import jenkins.model.Jenkins;

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
         */

        LinkedHashSet linkedHashSet = new LinkedHashSet<>(ConfigurationAsCode.get().getRootConfigurators());
        Iterator<RootElementConfigurator> i = linkedHashSet.iterator();
        while (i.hasNext()) {
            RootElementConfigurator rootElementConfigurator = i.next();
            schemaString.append("\"" + rootElementConfigurator.getName() + "\": {" + "\"type\": \"object\",\n" +
                    "    },");
        }
        schemaString.append("},\n");
        Set configObjects = new LinkedHashSet<>(ConfigurationAsCode.get().getConfigurators());
        System.out.println(configObjects.size());
        Iterator<Objects> obj = configObjects.iterator();

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
