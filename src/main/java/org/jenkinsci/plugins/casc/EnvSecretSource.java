package org.jenkinsci.plugins.casc;

import hudson.Extension;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Extension
public class EnvSecretSource extends SecretSource {

    @Override
    public String reveal(String fromKey) {
        // TODO I Wonder this could be done during parsing with some snakeyml extension
        Matcher m = SecretSource.SECRET_PATTERN.matcher(fromKey);
        if(m.matches()) {
            final String var = m.group(1);
            String config = System.getProperty(var, System.getenv(var));
            if (config == null) throw new IllegalStateException("Environment variable not set: "+var);
            return config;
        }
        return null;
    }
}
