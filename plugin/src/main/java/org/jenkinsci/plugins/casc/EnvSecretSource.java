package org.jenkinsci.plugins.casc;

import hudson.Extension;

import java.util.Optional;

@Extension
public class EnvSecretSource extends SecretSource {

    @Override
    public Optional<String> reveal(String envKey) {
        Optional<String> returnValue = Optional.empty();
        String config = System.getProperty(envKey, System.getenv(envKey));
        if (config != null) returnValue = Optional.of(config);
        return returnValue;
    }
}
