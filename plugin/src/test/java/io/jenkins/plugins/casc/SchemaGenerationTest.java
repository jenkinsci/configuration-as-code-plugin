package io.jenkins.plugins.casc;

import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.Util;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;


import java.io.IOException;

import static com.gargoylesoftware.htmlunit.HttpMethod.POST;
import static io.jenkins.plugins.casc.SchemaGeneration.generateSchema;

public class SchemaGenerationTest{


    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    public void downloadOldSchema() throws Exception {

        JenkinsRule.WebClient client = j.createWebClient();
        WebRequest request = new WebRequest(client.createCrumbedUrl("configuration-as-code/schema"), POST);
        WebResponse response = client.loadWebResponse(request);
        System.out.println(response);
    }


    @Test
    public void validateSchema() throws IOException {
        JSONObject schemaObject  = generateSchema();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(schemaObject.toString());
        String prettyJsonString = gson.toJson(je);
        System.out.println(prettyJsonString);
        
//        JSONObject jsonSchema = new JSONObject(
//            new JSONTokener(schema));

//        String yamlContents = "";
//        try {
//            yamlContents = Util.toStringFromYamlFile(this, "merge3.yml");
//        } catch (URISyntaxException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        System.out.println(yamlContents);
    }


    @Test
    public void checkRootConfigurators() {


    }

    @Test
    public void checkInitialTemplate() {

    }


}
