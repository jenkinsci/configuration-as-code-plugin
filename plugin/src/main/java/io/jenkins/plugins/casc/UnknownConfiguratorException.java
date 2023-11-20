package io.jenkins.plugins.casc;

import java.util.List;

public class UnknownConfiguratorException extends ConfiguratorException {

    private final List<String> configuratorNames;
    private final String errorMessage;

    public UnknownConfiguratorException(List<String> configuratorNames, String errorMessage) {
        super(errorMessage + String.join(", ", configuratorNames));
        this.errorMessage = errorMessage;
        this.configuratorNames = configuratorNames;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public List<String> getConfiguratorNames() {
        return configuratorNames;
    }
}
