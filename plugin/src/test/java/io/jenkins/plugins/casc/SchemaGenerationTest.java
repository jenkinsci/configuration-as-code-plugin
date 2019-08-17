package io.jenkins.plugins.casc;

import hudson.DescriptorExtensionList;
import hudson.model.UpdateSite;
import hudson.util.DirScanner;
import io.jenkins.plugins.casc.core.*;
import io.jenkins.plugins.casc.impl.configurators.DataBoundConfigurator;
import io.jenkins.plugins.casc.impl.configurators.GlobalConfigurationCategoryConfigurator;
import io.jenkins.plugins.casc.impl.configurators.HeteroDescribableConfigurator;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.Rule;
import org.junit.Test;
import java.util.*;


import java.util.LinkedHashSet;


public class SchemaGenerationTest{


    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    public void schemaShouldSucceed() {


        /**
         * Used to generate the schema for root configurators
         */
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
//        System.out.println(schemaString);

        /**
         * Used to generate the schema for the descriptors
         * Finds out the instance of each of the configurators
         * and gets the required descriptors from the instance method.
         */
        ConfigurationAsCode configurationAsCode = ConfigurationAsCode.get();
        configurationAsCode.getConfigurators()
                .stream()
                .forEach(configurator -> {

                    DescriptorExtensionList descriptorExtensionList = null;

                    if(configurator instanceof HeteroDescribableConfigurator) {
                        HeteroDescribableConfigurator heteroDescribableConfigurator = (HeteroDescribableConfigurator) configurator;
                        descriptorExtensionList = j.jenkins.getDescriptorList(heteroDescribableConfigurator.getTarget());

                    } else if (configurator instanceof DataBoundConfigurator){
                        DataBoundConfigurator dataBoundConfigurator = (DataBoundConfigurator) configurator;
                        descriptorExtensionList = j.jenkins.getDescriptorList(dataBoundConfigurator.getTarget());


                    }  else if (configurator instanceof JenkinsConfigurator) {
                        JenkinsConfigurator jenkinsConfigurator = (JenkinsConfigurator) configurator;
                        descriptorExtensionList = j.jenkins.getDescriptorList(jenkinsConfigurator.getTarget());

                    }  else if (configurator instanceof GlobalConfigurationCategoryConfigurator) {
                        GlobalConfigurationCategoryConfigurator globalConfigurationCategoryConfigurator = (GlobalConfigurationCategoryConfigurator) configurator;
                        descriptorExtensionList = j.jenkins.getDescriptorList(globalConfigurationCategoryConfigurator.getTarget());

                    }  else if (configurator instanceof UnsecuredAuthorizationStrategyConfigurator) {
                        UnsecuredAuthorizationStrategyConfigurator unsecuredAuthorizationStrategyConfigurator = (UnsecuredAuthorizationStrategyConfigurator) configurator;
                        descriptorExtensionList = j.jenkins.getDescriptorList(unsecuredAuthorizationStrategyConfigurator.getTarget());

                    } else if (configurator instanceof UpdateCenterConfigurator) {
                        UpdateCenterConfigurator updateCenterConfigurator = (UpdateCenterConfigurator) configurator;
                        descriptorExtensionList = j.jenkins.getDescriptorList(updateCenterConfigurator.getTarget());

                    } else if (configurator instanceof HudsonPrivateSecurityRealmConfigurator) {
                        HudsonPrivateSecurityRealmConfigurator hudsonPConfigurator = (HudsonPrivateSecurityRealmConfigurator) configurator;
                        descriptorExtensionList = j.jenkins.getDescriptorList(hudsonPConfigurator.getTarget());

                    } else if (configurator instanceof UpdateSiteConfigurator) {
                        UpdateSiteConfigurator updateSiteConfigurator = (UpdateSiteConfigurator) configurator;
                        descriptorExtensionList = j.jenkins.getDescriptorList(updateSiteConfigurator.getTarget());

                    } else if (configurator instanceof JNLPLauncherConfigurator) {
                        JNLPLauncherConfigurator jLaunchConfigurator = (JNLPLauncherConfigurator) configurator;
                        descriptorExtensionList = j.jenkins.getDescriptorList(jLaunchConfigurator.getTarget());
                    }

                    /**
                     * Iterate over the list and generate the schema
                     */

//
                    for (Object obj: descriptorExtensionList) {
                        /*
                         * Iterate over the obj list and construct the schema.
                         * */
                    }
                });

    }

}