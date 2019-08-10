package io.jenkins.plugins.casc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kjetland.jackson.jsonSchema.JsonSchemaGenerator;

public class SchemaGenerator {


    public String generateSchema() throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();
        JsonSchemaGenerator jsonSchemaGenerator = new JsonSchemaGenerator(objectMapper);
        JsonNode jsonSchema = jsonSchemaGenerator.generateJsonSchema(SchemaObjects.class);
        String jsonSchemaAsString = objectMapper.writeValueAsString(jsonSchema);
        return jsonSchemaAsString;

    }
}
