package io.jenkins.plugins.casc.permissions;

public enum Action {
    VIEW_CONFIGURATION("Export configuration"),
    APPLY_NEW_CONFIGURATION("Apply new configuration"),
    RELOAD_EXISTING_CONFIGURATION("Reload existing configuration"),
    ;

    String buttonText;

    Action(String buttonText) {
        this.buttonText = buttonText;
    }
}
