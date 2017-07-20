package org.jenkinsci.plugins.systemconfigdsl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.google.gson.JsonObject;
import org.jenkinsci.plugins.systemconfigdsl.error.ValidationException;

import java.util.logging.Logger;

public class Validator {
    private static final Logger LOGGER = Logger.getLogger(Validator.class.getName());

    private JsonSchema validationSchema;
    private String schemaResourceName;

    public Validator(final String schemaResourceName) {
        this.schemaResourceName = schemaResourceName;

        final ObjectMapper mapper = new ObjectMapper();
        final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
        final ClassLoader classLoader = getClass().getClassLoader();
        try {
            validationSchema = factory.getJsonSchema(
                    mapper.readTree(classLoader.getResourceAsStream(schemaResourceName))
            );
            LOGGER.fine("Validation schema loaded: " + schemaResourceName);
        } catch (Exception e) {
            final String message = "Cannot parse JSON schema. The resource: "
                    + schemaResourceName + ". " + e.getClass() + ":  " + e.getMessage();
            LOGGER.severe(message);
            throw new IllegalArgumentException(message, e);
        }
    }

    public void validate(final String jsonInput) throws ValidationException {
        try {
            final ProcessingReport report = validationSchema.validate(JsonLoader.fromString(jsonInput));
            if (!report.isSuccess()) {
                LOGGER.warning(report.toString());
                LOGGER.warning(jsonInput);
                throw new ValidationException(report.toString());
            }

            LOGGER.fine("VALIDATED. Schema used: " + schemaResourceName);
        } catch (Exception e) {
            final String message = "Cannot validate given JSON string: " + e.getMessage();
            LOGGER.fine(message);
            throw new ValidationException(message, e);
        }
    }

}
