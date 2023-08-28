package io.jenkins.plugins.casc;

import hudson.util.BootFailure;

public class ConfigurationAsCodeBootFailure extends BootFailure {

    public ConfigurationAsCodeBootFailure(Throwable cause) {
        super(cause);
    }
}
