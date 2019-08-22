package io.jenkins.plugins.casc;

import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import static com.gargoylesoftware.htmlunit.HttpMethod.POST;
import static io.jenkins.plugins.casc.SchemaGeneration.generateSchema;

public class SchemaGenerationTest{


    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    public void schemaShouldSucceed() throws IOException {

        /**
         *Validate the schema against a validator
         * or against the already defined schema.
         */
        String s = generateSchema();
        System.out.println(s);
//        String fileName = "Schema";
//        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
//        writer.write(s);
//        writer.close();
    }

    @Test
    public void downloadOldSchema() throws Exception {

        String fileName = "OldSchema";
        JenkinsRule.WebClient client = j.createWebClient();
        WebRequest request = new WebRequest(client.createCrumbedUrl("configuration-as-code/schema"), POST);
        WebResponse response = client.loadWebResponse(request);
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
        writer.write(response.getContentAsString()
        );
        writer.close();
    }
}
