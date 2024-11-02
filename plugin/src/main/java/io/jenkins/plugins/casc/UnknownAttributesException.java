package io.jenkins.plugins.casc;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.util.List;

public class UnknownAttributesException extends ConfiguratorException {

    private final String errorMessage;

    public UnknownAttributesException(
            @CheckForNull Configurator configurator,
            String simpleMessage,
            @CheckForNull String message,
            String invalidAttribute,
            List<String> validAttributes) {
        super(configurator, message, invalidAttribute, validAttributes, null);
        this.errorMessage = simpleMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
