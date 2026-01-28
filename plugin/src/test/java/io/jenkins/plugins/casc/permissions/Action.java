package io.jenkins.plugins.casc.permissions;

public enum Action {
    VIEW_CONFIGURATION("Export configuration"),
    APPLY_NEW_CONFIGURATION("Setup configuration"),
    ;

    String buttonText;

    Action(String buttonText) {
        this.buttonText = buttonText;
    }
}
