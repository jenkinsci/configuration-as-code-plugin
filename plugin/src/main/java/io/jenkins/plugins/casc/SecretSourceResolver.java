package io.jenkins.plugins.casc;

import com.google.common.collect.ImmutableMap;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.lookup.StringLookup;
import org.apache.commons.text.lookup.StringLookupFactory;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.DoNotUse;

import static io.vavr.API.unchecked;

/**
 * Resolves secret variables and converts escaped internal variables.
 */
public class SecretSourceResolver {
    private static final String enclosedBy = "${";
    private static final String enclosedIn = "}";
    private static final char escapedWith = '^';
    private static final String escapeEnclosedBy = escapedWith + enclosedBy;

    private static final Logger LOGGER = Logger.getLogger(SecretSourceResolver.class.getName());
    private final StringSubstitutor nullSubstitutor;
    private final StringSubstitutor substitutor;

    public SecretSourceResolver(ConfigurationContext configurationContext) {
        ConfigurationContext context = configurationContext;
        substitutor = new StringSubstitutor(
            StringLookupFactory.INSTANCE.interpolatorStringLookup(ImmutableMap
                    .of("base64", StringLookupFactory.INSTANCE.base64EncoderStringLookup(),
                        "file", FileStringLookup.INSTANCE
                    ),
                new ConfigurationContextStringLookup(context), false))
            .setEscapeChar(escapedWith)
            .setVariablePrefix(enclosedBy)
            .setVariableSuffix(enclosedIn)
            .setEnableSubstitutionInVariables(true)
            .setPreserveEscapes(true);
        nullSubstitutor = new StringSubstitutor(UnresolvedLookup.INSTANCE)
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
    public String encode(@CheckForNull String toEncode) {
        if (toEncode == null) {
            return null;
        }
        return toEncode.replace(enclosedBy, escapeEnclosedBy);
    }

    /**
     * Resolve string with potential secrets
     *
     * @param context Configuration context
     * @param toInterpolate potential variables that need to revealed
     * @return original string with any secrets that could be resolved if secrets could not be
     * resolved they will be defaulted to default value defined by ':-', otherwise default to empty
     * String. Secrets are defined as anything enclosed by '${}'
     * @since 1.42
     * @deprecated use ${link {@link ConfigurationContext#getSecretSourceResolver()#resolve(String)}} instead.
     */
    @Deprecated
    @Restricted(DoNotUse.class)
    public static String resolve(ConfigurationContext context, String toInterpolate) {
        return context.getSecretSourceResolver().resolve(toInterpolate);
    }

    /**
     * Resolve string with potential secrets
     *
     * @param toInterpolate potential variables that need to revealed
     * @return original string with any secrets that could be resolved if secrets could not be
     * resolved they will be defaulted to default value defined by ':-', otherwise default to empty
     * String. Secrets are defined as anything enclosed by '${}'
     */
    public String resolve(String toInterpolate) {
        if (StringUtils.isBlank(toInterpolate) || !toInterpolate.contains(enclosedBy)) {
            return toInterpolate;
        }
        String text = substitutor.replace(toInterpolate);
        return nullSubstitutor.replace(text);
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

        private final ConfigurationContext context;

        private ConfigurationContextStringLookup(ConfigurationContext context) {
            this.context = context;
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
        public String lookup(@NonNull final String key) {
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
