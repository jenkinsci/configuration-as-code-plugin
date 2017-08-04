package org.jenkinsci.plugins.systemconfigdsl.error;

public class ConfigurationNotFoundException extends Exception {
    public ConfigurationNotFoundException(String message) {
        super(message);
    }

    public ConfigurationNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
