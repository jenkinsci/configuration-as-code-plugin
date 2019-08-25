package io.jenkins.plugins.casc;

import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.Util;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static com.gargoylesoftware.htmlunit.HttpMethod.POST;
import static io.jenkins.plugins.casc.SchemaGeneration.generateSchema;

public class SchemaGenerationTest {


    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    public void downloadOldSchema() throws Exception {

        JenkinsRule.WebClient client = j.createWebClient();
        WebRequest request = new WebRequest(client.createCrumbedUrl("configuration-as-code/schema"),
            POST);
        WebResponse response = client.loadWebResponse(request);
        System.out.println(response);
    }


    @Test
    public void validSchemaShouldSucceed() throws Exception {

        JSONObject schemaObject = generateSchema();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jsonParser = new JsonParser();
        JsonElement jsonElement = jsonParser.parse(schemaObject.toString());
        String prettyJsonString = gson.toJson(jsonElement);

        System.out.println(prettyJsonString);

        JSONObject jsonSchema = new JSONObject(
            new JSONTokener(prettyJsonString));

        String yamlStringContents = Util.toStringFromYamlFile(this, "validSchemaConfig.yml");
        JSONObject jsonSubject = new JSONObject(
            new JSONTokener(Util.convertToJson(yamlStringContents)));
        Schema schema = SchemaLoader.load(jsonSchema);
        schema.validate(jsonSubject);
    }

    @Test
    public void invalidSchemaShouldNotSucceed() throws Exception {

        JSONObject schemaObject = generateSchema();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jsonParser = new JsonParser();
        JsonElement jsonElement = jsonParser.parse(schemaObject.toString());
        String prettyJsonString = gson.toJson(jsonElement);
        JSONObject jsonSchema = new JSONObject(
            new JSONTokener(prettyJsonString));
        String yamlStringContents = Util.toStringFromYamlFile(this, "invalidSchemaConfig.yml");
        System.out.println(Util.convertToJson(yamlStringContents));
        JSONObject jsonSubject = new JSONObject(
            new JSONTokener(Util.convertToJson(yamlStringContents)));
        Schema schema = SchemaLoader.load(jsonSchema);
        schema.validate(jsonSubject);
    }





    @Test
    public void checkRootConfigurators() {

    }

    @Test
    public void checkInitialTemplate() {

    }



}
