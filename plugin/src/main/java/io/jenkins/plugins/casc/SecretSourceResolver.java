package io.jenkins.plugins.casc;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.lookup.StringLookup;
import org.apache.commons.text.lookup.StringLookupFactory;

import static io.vavr.API.unchecked;

/**
 * Resolves secret variables and converts escaped internal variables.
 */
public class SecretSourceResolver {
    private static final String enclosedBy = "${";
    private static final String enclosedIn = "}";
    private static final char escapedWith = '^';

    private static final Logger LOGGER = Logger.getLogger(SecretSourceResolver.class.getName());
    private static final StringSubstitutor NULL_SUBSTITUTOR = new StringSubstitutor(
        UnresolvedLookup.INSTANCE);
    private static final StringSubstitutor SUBSTITUTOR = new StringSubstitutor(
        StringLookupFactory.INSTANCE.interpolatorStringLookup(
            ImmutableMap.of(
                "base64", StringLookupFactory.INSTANCE.base64EncoderStringLookup(),
                "file", FileStringLookup.INSTANCE
            ),
            ConfigurationContextStringLookup.INSTANCE, false));

    static {
        SUBSTITUTOR
            .setEscapeChar(escapedWith)
            .setVariablePrefix(enclosedBy)
            .setVariableSuffix(enclosedIn)
            .setEnableSubstitutionInVariables(true)
            .setPreserveEscapes(true);
        NULL_SUBSTITUTOR
            .setEscapeChar(escapedWith)
            .setVariablePrefix(enclosedBy)
            .setVariableSuffix(enclosedIn);
    }

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
        return toEncode.replace(enclosedBy, "^${");
    }

    public static String resolve(ConfigurationContext context, String toInterpolate) {
        ConfigurationContextStringLookup.INSTANCE.context = context;
        String text = SUBSTITUTOR.replace(toInterpolate);
        return NULL_SUBSTITUTOR.replace(text);
    }

    static class UnresolvedLookup implements StringLookup {

        static final UnresolvedLookup INSTANCE = new UnresolvedLookup();

        private UnresolvedLookup() {
        }

        @Override
        public String lookup(String key) {
            LOGGER.log(Level.WARNING, String.format(
                "Configuration import: Found unresolved variable '%s'. Will default to empty string",
                key));
            return "";
        }
    }

    static class ConfigurationContextStringLookup implements StringLookup {

        static final ConfigurationContextStringLookup INSTANCE = new ConfigurationContextStringLookup();

        private ConfigurationContext context;

        private ConfigurationContextStringLookup() {
        }

        @Override
        public String lookup(String key) {
            return context.getSecretSources().stream()
                .map(source -> unchecked(() -> source.reveal(key)).apply())
                .flatMap(o -> o.map(Stream::of).orElseGet(Stream::empty))
                .findFirst()
                .orElse(null);
        }
    }

    static class FileStringLookup implements StringLookup {

        static final FileStringLookup INSTANCE = new FileStringLookup();

        @Override
        public String lookup(final String key) {
            if (key == null) {
                return null;
            }
            try {
                return new String(Files.readAllBytes(Paths.get(key)), StandardCharsets.UTF_8);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, String.format(
                    "Configuration import: Error looking up file '%s' with UTF-8 encoding. Will default to empty string",
                    key), e);
                return null;
            }
        }
    }
}
