package io.jenkins.plugins.casc;

import hudson.ExtensionPoint;
import jenkins.model.Jenkins;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.Beta;

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
@Restricted(Beta.class)
public abstract class SecretSource implements ExtensionPoint {

    public static final Pattern SECRET_PATTERN = Pattern.compile("\\$\\{([^:\\s]*)(?::-)?([^}\\s]*)?\\}");

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

    public static Optional<String> defaultValue(String key) {
        Matcher m = SECRET_PATTERN.matcher(key);
        if(m.matches() && m.groupCount() == 2) {
            return Optional.ofNullable(m.group(2));
        }
        return Optional.empty();
    }

}
