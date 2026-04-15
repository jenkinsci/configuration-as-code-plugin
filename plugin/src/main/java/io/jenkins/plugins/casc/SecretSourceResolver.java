package io.jenkins.plugins.casc;

import static io.vavr.API.unchecked;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.lookup.StringLookup;
import org.json.JSONObject;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.DoNotUse;

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
        Map<String, StringLookup> map = Map.of(
                "base64",
                Base64Lookup.INSTANCE,
                "fileBase64",
                FileBase64Lookup.INSTANCE,
                "readFileBase64",
                FileBase64Lookup.INSTANCE,
                "file",
                FileStringLookup.INSTANCE,
                "readFile",
                FileStringLookup.INSTANCE,
                "sysProp",
                SystemPropertyLookup.INSTANCE,
                "decodeBase64",
                DecodeBase64Lookup.INSTANCE,
                "json",
                JsonLookup.INSTANCE,
                "trim",
                TrimLookup.INSTANCE);

        substitutor = new StringSubstitutor(new FixedInterpolatorStringLookup(
                        map, new ConfigurationContextStringLookup(configurationContext)))
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
     * @deprecated use {@link #resolve(String)}} instead.
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
        String result = substitutor.replace(toInterpolate);
        result = nullSubstitutor.replace(result);
        return result;
    }

    static class UnresolvedLookup implements StringLookup {

        static final UnresolvedLookup INSTANCE = new UnresolvedLookup();

        private static final String STRICT_MODE_ENV = "CASC_STRICT_SECRET_RESOLUTION";
        private static final String STRICT_MODE_PROP = "casc.strict.secret.resolution";

        private UnresolvedLookup() {}

        @Override
        public String lookup(String key) {
            boolean isStrict =
                    Boolean.parseBoolean(System.getProperty(STRICT_MODE_PROP, System.getenv(STRICT_MODE_ENV)));

            if (isStrict) {
                throw new IllegalStateException(
                        String.format("Unable to resolve variable '%s'. Aborting configuration reload.", key));
            }

            LOGGER.log(
                    Level.WARNING,
                    String.format(
                            "Configuration import: Found unresolved variable '%s'. Will default to empty string", key));
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

    static class SystemPropertyLookup implements StringLookup {

        static final SystemPropertyLookup INSTANCE = new SystemPropertyLookup();

        @Override
        public String lookup(@NonNull final String key) {
            final String output = System.getProperty(key);
            if (output == null) {
                LOGGER.log(
                        Level.WARNING,
                        String.format(
                                "Configuration import: System Properties did not contain the specified key '%s'. Will default to empty string.",
                                key));
                return "";
            }
            return output;
        }
    }

    static class FileStringLookup implements StringLookup {

        static final FileStringLookup INSTANCE = new FileStringLookup();

        @Override
        public String lookup(@NonNull final String key) {
            try {
                return new String(Files.readAllBytes(Paths.get(key)), StandardCharsets.UTF_8);
            } catch (IOException | InvalidPathException e) {
                LOGGER.log(
                        Level.WARNING,
                        String.format(
                                "Configuration import: Error looking up file '%s' with UTF-8 encoding. Will default to empty string",
                                key),
                        e);
                return null;
            }
        }
    }

    static class Base64Lookup implements StringLookup {

        static final Base64Lookup INSTANCE = new Base64Lookup();

        @Override
        public String lookup(@NonNull final String key) {
            return Base64.getEncoder().encodeToString(key.getBytes(StandardCharsets.UTF_8));
        }
    }

    static class DecodeBase64Lookup implements StringLookup {

        static final DecodeBase64Lookup INSTANCE = new DecodeBase64Lookup();

        @Override
        public String lookup(@NonNull final String key) {
            return new String(Base64.getDecoder().decode(key.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
        }
    }

    static class FileBase64Lookup implements StringLookup {

        static final FileBase64Lookup INSTANCE = new FileBase64Lookup();

        @Override
        public String lookup(@NonNull final String key) {
            try {
                byte[] fileContent = Files.readAllBytes(Paths.get(key));
                return Base64.getEncoder().encodeToString(fileContent);
            } catch (IOException | InvalidPathException e) {
                LOGGER.log(
                        Level.WARNING,
                        String.format(
                                "Configuration import: Error looking up file '%s'. Will default to empty string", key),
                        e);
                return null;
            }
        }
    }

    static class JsonLookup implements StringLookup {

        static final JsonLookup INSTANCE = new JsonLookup();

        private JsonLookup() {}

        @Override
        public String lookup(@NonNull final String key) {
            final String[] components = key.split(":", 2);
            final String jsonFieldName = components[0];
            final String json = components[1];
            final JSONObject root = new JSONObject(json);

            final String output = root.optString(jsonFieldName, null);
            if (output == null) {
                LOGGER.log(
                        Level.WARNING,
                        String.format(
                                "Configuration import: JSON secret did not contain the specified key '%s'. Will default to empty string.",
                                jsonFieldName));
                return "";
            }
            return output;
        }
    }

    static class TrimLookup implements StringLookup {

        static final TrimLookup INSTANCE = new TrimLookup();

        private TrimLookup() {}

        @Override
        public String lookup(@NonNull final String key) {
            return StringUtils.stripEnd(key, null);
        }
    }
}
