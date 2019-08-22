package io.jenkins.plugins.casc;

import hudson.DescriptorExtensionList;
import io.jenkins.plugins.casc.impl.configurators.DataBoundConfigurator;
import io.jenkins.plugins.casc.impl.configurators.HeteroDescribableConfigurator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import jenkins.model.Jenkins;

public class SchemaGeneration {

    public String generateSchema() {

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
        configurationAsCode.getConfigurators()
                .stream()
                .forEach(configurator -> {

                    DescriptorExtensionList descriptorExtensionList = null;
                    System.out.println(configurator.getClass().getName());
                    if(configurator instanceof HeteroDescribableConfigurator) {
                        HeteroDescribableConfigurator heteroDescribableConfigurator = (HeteroDescribableConfigurator) configurator;
                        descriptorExtensionList = Jenkins.getInstance().getDescriptorList(heteroDescribableConfigurator.getTarget());

                    } else if (configurator instanceof DataBoundConfigurator) {
                        DataBoundConfigurator dataBoundConfigurator = (DataBoundConfigurator) configurator;
                        descriptorExtensionList = Jenkins.getInstance().getDescriptorList(dataBoundConfigurator.getTarget());
                    }

//                      else if (configurator instanceof JenkinsConfigurator) {
//                        JenkinsConfigurator jenkinsConfigurator = (JenkinsConfigurator) configurator;
//                        descriptorExtensionList = Jenkins.getInstance().getDescriptorList(jenkinsConfigurator.getTarget());
//
//                    }
//
//                    else if (configurator instanceof GlobalConfigurationCategoryConfigurator) {
//                        GlobalConfigurationCategoryConfigurator globalConfigurationCategoryConfigurator = (GlobalConfigurationCategoryConfigurator) configurator;
//                        descriptorExtensionList = Jenkins.getInstance().getDescriptorList(globalConfigurationCategoryConfigurator.getTarget());
//
//                    }  else if (configurator instanceof UnsecuredAuthorizationStrategyConfigurator) {
//                        UnsecuredAuthorizationStrategyConfigurator unsecuredAuthorizationStrategyConfigurator = (UnsecuredAuthorizationStrategyConfigurator) configurator;
//                        descriptorExtensionList = Jenkins.getInstance().getDescriptorList(unsecuredAuthorizationStrategyConfigurator.getTarget());
//
//                    } else if (configurator instanceof UpdateCenterConfigurator) {
//                        UpdateCenterConfigurator updateCenterConfigurator = (UpdateCenterConfigurator) configurator;
//                        descriptorExtensionList = Jenkins.getInstance().getDescriptorList(updateCenterConfigurator.getTarget());
//
//                    } else if (configurator instanceof HudsonPrivateSecurityRealmConfigurator) {
//                        HudsonPrivateSecurityRealmConfigurator hudsonPConfigurator = (HudsonPrivateSecurityRealmConfigurator) configurator;
//                        descriptorExtensionList = Jenkins.getInstance().getDescriptorList(hudsonPConfigurator.getTarget());
//
//                    } else if (configurator instanceof UpdateSiteConfigurator) {
//                        UpdateSiteConfigurator updateSiteConfigurator = (UpdateSiteConfigurator) configurator;
//                        descriptorExtensionList = Jenkins.getInstance().getDescriptorList(updateSiteConfigurator.getTarget());
//
//                    } else if (configurator instanceof JNLPLauncherConfigurator) {
//                        JNLPLauncherConfigurator jLaunchConfigurator = (JNLPLauncherConfigurator) configurator;
//                        descriptorExtensionList = Jenkins.getInstance().getDescriptorList(jLaunchConfigurator.getTarget());
//
//                    } else if (configurator instanceof AdminWhitelistRuleConfigurator) {
//                        AdminWhitelistRuleConfigurator adminWhitelistRuleConfigurator = (AdminWhitelistRuleConfigurator) configurator;
//                        descriptorExtensionList = Jenkins.getInstance().getDescriptorList(adminWhitelistRuleConfigurator.getTarget());
//
//                    } else if(configurator instanceof MavenConfigurator ) {
//                        MavenConfigurator mavenConfigurator = (MavenConfigurator) configurator;
//                        descriptorExtensionList = Jenkins.getInstance().getDescriptorList(mavenConfigurator.getTarget());
//                    }
                    /**
                     * Iterate over the list and generate the schema
                     */

                    for (Object obj: descriptorExtensionList) {
                        schemaString.append("{\n" +
                                "      \"properties\" : {" +
                                "      \"" + obj.getClass().getName() +"\"" + ": { \"$ref\" : \"#/definitions/" +
                                obj.toString() + "\" }" +
                                " }");
                    }
                });

            return schemaString.toString();
    }
}
