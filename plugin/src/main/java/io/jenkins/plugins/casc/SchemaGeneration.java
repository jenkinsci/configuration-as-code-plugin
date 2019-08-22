package io.jenkins.plugins.casc;

import hudson.DescriptorExtensionList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import jenkins.model.Jenkins;

public class SchemaGeneration {

    public static String generateSchema() {

        LinkedHashSet linkedHashSet = new LinkedHashSet<>(ConfigurationAsCode.get().getRootConfigurators());
        Iterator<RootElementConfigurator> i = linkedHashSet.iterator();

        StringBuilder schemaString = new StringBuilder();
        schemaString.append("{\n" +
                "  \"$schema\": \"http://json-schema.org/draft-06/schema#\",\n" +
                "  \"id\": \"http://jenkins.io/configuration-as-code#\",\n" +
                "  \"description\": \"Jenkins Configuration as Code\",\n" +
                "  \"type\": \"object\",\n" +
                "  \"properties\": {\n" +
                "");

        while (i.hasNext()) {

            List<Attribute> attributeList = new ArrayList<>(i.next().getAttributes());
            for (Attribute a : attributeList) {
                String type;
                if (a.type.toString().equals("int")) {
                    type = "int";
                } else if (a.type.toString().equals("class java.lang.String")) {
                    type = "string";
                } else if (a.type.toString().equals("boolean")) {
                    type = "boolean";
                } else {
                    type = "object";
                }
                schemaString.append(a.name + ": " + "{\n" +
                        "      \"type\": \"" + type + "\",\n" +
                        "      \"$ref\": \"#/definitions/" + a.type.getName() + "\"\n" +
                        "    },");
            }
        }
        System.out.println(schemaString);

        /**
         * Used to generate the schema for the descriptors
         * Finds out the instance of each of the configurators
         * and gets the required descriptors from the instance method.
         * Appending the oneOf tag to the schema.
         */

        schemaString.append("schema\" : {\n" +
                "    \"oneOf\": [");

        ConfigurationAsCode configurationAsCode = ConfigurationAsCode.get();
        for (Object configurator : configurationAsCode.getConfigurators()) {
            DescriptorExtensionList descriptorExtensionList = null;
            if (configurator instanceof Configurator<?>) {
                Configurator c = (Configurator) configurator;
                descriptorExtensionList = Jenkins.getInstance()
                    .getDescriptorList(c.getTarget());

            }

            /**
             * Iterate over the list and generate the schema
             */

            for (Object obj : descriptorExtensionList) {
                schemaString.append("{\n" +
                    "      \"properties\" : {\n" +
                    "      \"" + obj.getClass().getName() + "\"" + ": { \"$ref\" : \"#/definitions/"
                    +
                    obj.toString() + "\" }\n" +
                    " }");
            }
        }

        return schemaString.toString();
    }
}
