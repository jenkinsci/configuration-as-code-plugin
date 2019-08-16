package io.jenkins.plugins.casc;

import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.apache.log4j.spi.Configurator;
import org.junit.Rule;
import org.junit.Test;
import java.util.*;


import java.io.IOException;
import java.util.LinkedHashSet;

import static org.junit.Assert.*;

public class SchemaGenerationTest {


    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();


    @Test
    public void schemaShouldSucceed() {

        LinkedHashSet linkedHashSet = new LinkedHashSet <> (ConfigurationAsCode.get().getRootConfigurators());
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
            for (Attribute a :attributeList) {
                String type;
                
                if(a.type.toString().equals("int")){
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
    }
}