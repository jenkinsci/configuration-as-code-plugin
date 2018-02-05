package org.jenkinsci.plugins.casc;

import hudson.ExtensionPoint;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves variable references in configuration file of the form "${abc}"
 *
 * <p>
 * Variable references are meant to hide secrets from configuration files.
 */
public abstract class SecretSource implements ExtensionPoint {
    /**
     *
     * @param secret
     * @return the revealed secret. Null in the case that the implementation is not replacing anything. Throws exception
     * if the secret could not be fetched.
     */
    public static final Pattern SECRET_PATTERN = Pattern.compile("\\$\\{(.*)\\}");

    //We need to compile the matcher once for every key we examine.
    public static Optional<String> requiresReveal(String key) {
        Matcher m = SECRET_PATTERN.matcher(key);
        if(m.matches()) {
            return Optional.of(m.group(1));
        }
        return Optional.empty();
    }

    public abstract Optional<String> reveal(String secret) throws IOException;

    public static List<SecretSource> all() {
        List<SecretSource> all = new ArrayList<>();
        all.addAll(Jenkins.getInstance().getExtensionList(SecretSource.class));
        return all;
    }

}
