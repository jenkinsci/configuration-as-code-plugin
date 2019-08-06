package io.jenkins.plugins.casc;

import io.vavr.Tuple;
import io.vavr.control.Try;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import org.apache.commons.lang.text.StrLookup;
import org.apache.commons.lang.text.StrSubstitutor;

import static io.vavr.API.unchecked;

/**
 * Resolves secret variables and converts escaped internal variables.
 */
public class SecretSourceResolver {
    private static final String enclosedBy = "${";
    private static final String enclosedIn = "}";
    private static final char escapedWith = '^';
    private static final String defaultDelimiter = ":-";

    private static final Logger LOGGER = Logger.getLogger(SecretSourceResolver.class.getName());

    /**
     * Encodes String so that it can be safely represented in the YAML after export.
     * @param toEncode String to encode
     * @return Encoded string
     * @since 1.25
     */
    public static String encode(@CheckForNull String toEncode) {
        if (toEncode == null) {
            return null;
        }
        return toEncode.replace("${", "^${");
    }

    public static String resolve(ConfigurationContext context, String toInterpolate) {
        return substitutor(context).replace(toInterpolate);
    }

    private static StrSubstitutor substitutor(ConfigurationContext context) {
        StrSubstitutor substitutor = new StrSubstitutor(new ConfigurationContextStrLookup(context));
        substitutor.setEscapeChar(escapedWith);
        substitutor.setVariablePrefix(enclosedBy);
        substitutor.setVariableSuffix(enclosedIn);
        return substitutor;
    }

    private static String handleJenkinsVariableDeclaration(ConfigurationContext context, String captured) {
        return enclosedBy + captured + enclosedIn;
    }

    private static class ConfigurationContextStrLookup extends StrLookup {

        private final ConfigurationContext context;

        public ConfigurationContextStrLookup(ConfigurationContext context) {
            this.context = context;
        }

        @Override
        public String lookup(String key) {
            String[] split = key.split(defaultDelimiter, 2);
            return Tuple.of(split[0], Try.of(() -> split[1]).toJavaOptional()).apply(
                    (toReveal, defaultValue) -> reveal(context, toReveal)
                            .map(Optional::of)
                            .orElse(defaultValue)
                            .orElseGet(() -> handleUndefinedVariable(key)));
        }
    }

    private static String handleUndefinedVariable(String captured) {
        LOGGER.log(Level.WARNING, "Configuration import: Found unresolved variable {0}. " +
                "Will default to empty string", captured);
        return "";
    }

    private static Optional<String> reveal(ConfigurationContext context, String captured) {
        return context.getSecretSources().stream()
                .map(source -> unchecked(() -> source.reveal(captured)).apply())
                .flatMap(o -> o.map(Stream::of).orElseGet(Stream::empty))
                .findFirst();
    }
}
