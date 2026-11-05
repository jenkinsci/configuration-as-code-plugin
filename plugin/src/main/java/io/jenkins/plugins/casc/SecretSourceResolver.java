package io.jenkins.plugins.casc;

import static io.vavr.API.unchecked;
import static java.lang.Boolean.TRUE;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
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

    private static final String PLACEHOLDER_PREFIX = "\u0000CASC_DECRYPT_";

    private static final String PLACEHOLDER_SUFFIX = "\u0000";

    private static final ThreadLocal<Map<String, String>> DECRYPT_PLACEHOLDERS = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> CURRENT_INPUT_DECRYPTED = new ThreadLocal<>();

    static boolean isDecryptedInput() {
        return TRUE.equals(CURRENT_INPUT_DECRYPTED.get());
    }

    private final StringSubstitutor nullSubstitutor;
    private final StringSubstitutor substitutor;

    public SecretSourceResolver(ConfigurationContext configurationContext) {
        Map<String, StringLookup> map = new HashMap<>();
        map.put("base64", new PlaceholderAwareLookup(Base64Lookup.INSTANCE));
        map.put("fileBase64", new PlaceholderAwareLookup(FileBase64Lookup.INSTANCE));
        map.put("readFileBase64", new PlaceholderAwareLookup(FileBase64Lookup.INSTANCE));
        map.put("file", new PlaceholderAwareLookup(FileStringLookup.INSTANCE));
        map.put("readFile", new PlaceholderAwareLookup(FileStringLookup.INSTANCE));
        map.put("sysProp", new PlaceholderAwareLookup(SystemPropertyLookup.INSTANCE));
        map.put("decodeBase64", new PlaceholderAwareLookup(DecodeBase64Lookup.INSTANCE));
        map.put("json", new PlaceholderAwareLookup(JsonLookup.INSTANCE));
        map.put("trim", new PlaceholderAwareLookup(TrimLookup.INSTANCE));
        map.put("decrypt", PlaceholderDecryptLookup.INSTANCE);

        substitutor = new StringSubstitutor(new FixedInterpolatorStringLookup(
                        map, new PlaceholderAwareLookup(new ConfigurationContextStringLookup(configurationContext))))
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

    static class PlaceholderDecryptLookup implements StringLookup {

        static final PlaceholderDecryptLookup INSTANCE = new PlaceholderDecryptLookup();

        private PlaceholderDecryptLookup() {}

        @Override
        public String lookup(@NonNull final String key) {
            Map<String, String> placeholderMap = DECRYPT_PLACEHOLDERS.get();
            if (placeholderMap == null) {
                throw new IllegalStateException(
                        "Decrypt lookup must be invoked through SecretSourceResolver.resolve()");
            }

            String plaintext = DecryptLookup.INSTANCE.lookup(key);
            String placeholder = PLACEHOLDER_PREFIX + UUID.randomUUID() + PLACEHOLDER_SUFFIX;
            placeholderMap.put(placeholder, plaintext);
            return placeholder;
        }
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

        Map<String, String> previous = DECRYPT_PLACEHOLDERS.get();
        Map<String, String> placeholders = new HashMap<>();
        DECRYPT_PLACEHOLDERS.set(placeholders);

        try {
            String result = substitutor.replace(toInterpolate);
            result = nullSubstitutor.replace(result);

            return restorePlaceholders(result, placeholders).text();
        } finally {
            if (previous == null) {
                DECRYPT_PLACEHOLDERS.remove();
            } else {
                DECRYPT_PLACEHOLDERS.set(previous);
            }
        }
    }

    record RestorationResult(String text, boolean modified) {}

    static RestorationResult restorePlaceholders(String text, Map<String, String> placeholders) {
        if (text == null || placeholders == null || placeholders.isEmpty()) {
            return new RestorationResult(text, false);
        }

        int prefixIdx = text.indexOf(PLACEHOLDER_PREFIX);
        if (prefixIdx == -1) {
            return new RestorationResult(text, false);
        }

        StringBuilder sb = new StringBuilder(text.length());
        int cursor = 0;
        boolean modified = false;

        while (prefixIdx != -1) {
            sb.append(text, cursor, prefixIdx);
            int suffixIdx = text.indexOf(PLACEHOLDER_SUFFIX, prefixIdx + PLACEHOLDER_PREFIX.length());
            if (suffixIdx != -1) {
                String candidate = text.substring(prefixIdx, suffixIdx + PLACEHOLDER_SUFFIX.length());
                String replacement = placeholders.get(candidate);
                if (replacement != null) {
                    sb.append(replacement);
                    cursor = suffixIdx + PLACEHOLDER_SUFFIX.length();
                    modified = true;
                } else {
                    sb.append(candidate);
                    cursor = suffixIdx + PLACEHOLDER_SUFFIX.length();
                }
            } else {
                sb.append(text, prefixIdx, text.length());
                cursor = text.length();
                break;
            }
            prefixIdx = text.indexOf(PLACEHOLDER_PREFIX, cursor);
        }

        if (cursor < text.length()) {
            sb.append(text, cursor, text.length());
        }

        return new RestorationResult(sb.toString(), modified);
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
            if (isDecryptedInput()) {
                return null;
            }
            return context.getSecretSources().stream()
                    .map(source -> unchecked(() -> source.reveal(key)).apply())
                    .flatMap(Optional::stream)
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
                if (isDecryptedInput()) {
                    LOGGER.log(
                            Level.WARNING,
                            "Configuration import: System Properties did not contain the specified key. Will default to empty string.");
                } else {
                    LOGGER.log(
                            Level.WARNING,
                            String.format(
                                    "Configuration import: System Properties did not contain the specified key '%s'. Will default to empty string.",
                                    key));
                }
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
                if (isDecryptedInput()) {
                    LOGGER.log(
                            Level.WARNING,
                            "Configuration import: Error looking up file with UTF-8 encoding. Will default to empty string");
                } else {
                    LOGGER.log(
                            Level.WARNING,
                            String.format(
                                    "Configuration import: Error looking up file '%s' with UTF-8 encoding. Will default to empty string",
                                    key),
                            e);
                }
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
                if (isDecryptedInput()) {
                    LOGGER.log(
                            Level.WARNING, "Configuration import: Error looking up file. Will default to empty string");
                } else {
                    LOGGER.log(
                            Level.WARNING,
                            String.format(
                                    "Configuration import: Error looking up file '%s'. Will default to empty string",
                                    key),
                            e);
                }
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
            if (components.length < 2) {
                if (isDecryptedInput()) {
                    LOGGER.log(
                            Level.WARNING,
                            "Configuration import: Invalid JSON lookup syntax. Will default to empty string.");
                } else {
                    LOGGER.log(
                            Level.WARNING,
                            String.format(
                                    "Configuration import: Invalid JSON lookup syntax '%s'. Will default to empty string.",
                                    key));
                }
                return "";
            }
            final String jsonFieldName = components[0];
            final String json = components[1];
            final JSONObject root = new JSONObject(json);

            final String output = root.optString(jsonFieldName, null);
            if (output == null) {
                if (isDecryptedInput()) {
                    LOGGER.log(
                            Level.WARNING,
                            "Configuration import: JSON secret did not contain the specified key. Will default to empty string.");
                } else {
                    LOGGER.log(
                            Level.WARNING,
                            String.format(
                                    "Configuration import: JSON secret did not contain the specified key '%s'. Will default to empty string.",
                                    jsonFieldName));
                }
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

    static class PlaceholderAwareLookup implements StringLookup {
        private final StringLookup delegate;

        PlaceholderAwareLookup(StringLookup delegate) {
            this.delegate = delegate;
        }

        @Override
        public String lookup(String key) {
            if (key == null) {
                return null;
            }

            Map<String, String> placeholderMap = DECRYPT_PLACEHOLDERS.get();
            if (placeholderMap == null || placeholderMap.isEmpty()) {
                return delegate.apply(key);
            }

            RestorationResult unwrapped = restorePlaceholders(key, placeholderMap);
            if (!unwrapped.modified()) {
                return delegate.apply(key);
            }

            Boolean previousDecrypted = CURRENT_INPUT_DECRYPTED.get();
            CURRENT_INPUT_DECRYPTED.set(true);
            try {
                String result = delegate.apply(unwrapped.text());
                String placeholder = PLACEHOLDER_PREFIX + UUID.randomUUID() + PLACEHOLDER_SUFFIX;
                placeholderMap.put(placeholder, result != null ? result : "");
                return placeholder;
            } catch (Exception e) {
                throw new IllegalStateException("Failed to evaluate helper with decrypted secret");
            } finally {
                if (previousDecrypted == null) {
                    CURRENT_INPUT_DECRYPTED.remove();
                } else {
                    CURRENT_INPUT_DECRYPTED.set(previousDecrypted);
                }
            }
        }
    }
}
