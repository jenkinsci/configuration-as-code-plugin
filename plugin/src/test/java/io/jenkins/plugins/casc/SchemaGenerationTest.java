package io.jenkins.plugins.casc;

import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.Rule;
import org.junit.Test;

import static io.jenkins.plugins.casc.SchemaGeneration.generateSchema;

public class SchemaGenerationTest{


    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    public void schemaShouldSucceed() {



        /**
         *Validate the schema against a validator
         * or against the already defined schema.
         */
        String s = generateSchema();
        System.out.println(s);
    }
}
