package io.jenkins.plugins.casc;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.text.lookup.StringLookup;
import org.apache.commons.text.lookup.StringLookupFactory;

/**
 * Proxies other {@link StringLookup}s using a keys within ${} markers using the format "${StringLookup:Key}".
 * <p>
 * Uses the {@link StringLookupFactory default lookups}.
 * </p>
 */
class FixedInterpolatorStringLookup implements StringLookup {

    /**
     * Defines the singleton for this class.
     *
     * @since 1.6
     */
    static final FixedInterpolatorStringLookup INSTANCE = new FixedInterpolatorStringLookup();

    /** Constant for the prefix separator. */
    private static final char PREFIX_SEPARATOR = ':';

    static String toKey(final String key) {
        return key.toLowerCase(Locale.ROOT);
    }

    /** The default string lookup. */
    private final StringLookup defaultStringLookup;

    /** The map of String lookups keyed by prefix. */
    private final Map<String, StringLookup> stringLookupMap;

    /**
     * Creates an instance using only lookups that work without initial properties and are stateless.
     * <p>
     * Uses the {@link StringLookupFactory default lookups}.
     * </p>
     */
    FixedInterpolatorStringLookup() {
        this((Map<String, String>) null);
    }

    /**
     * Creates a fully customized instance.
     *
     * @param stringLookupMap the map of string lookups.
     * @param defaultStringLookup the default string lookup.
     * @param addDefaultLookups whether the default lookups should be used.
     */
    FixedInterpolatorStringLookup(final Map<String, StringLookup> stringLookupMap, final StringLookup defaultStringLookup,
        final boolean addDefaultLookups) {
        super();
        this.defaultStringLookup = defaultStringLookup;
        this.stringLookupMap = new HashMap<>(stringLookupMap.size());
        for (final Entry<String, StringLookup> entry : stringLookupMap.entrySet()) {
            this.stringLookupMap.put(toKey(entry.getKey()), entry.getValue());
        }
        if (addDefaultLookups) {
            StringLookupFactory.INSTANCE.addDefaultStringLookups(this.stringLookupMap);
        }
    }

    /**
     * Creates an instance using only lookups that work without initial properties and are stateless.
     * <p>
     * Uses the {@link StringLookupFactory default lookups}.
     * </p>
     *
     * @param <V> the map's value type.
     * @param defaultMap the default map for string lookups.
     */
    <V> FixedInterpolatorStringLookup(final Map<String, V> defaultMap) {
        this(StringLookupFactory.INSTANCE.mapStringLookup(defaultMap == null ? new HashMap<String, V>() : defaultMap));
    }

    /**
     * Creates an instance with the given lookup.
     *
     * @param defaultStringLookup the default lookup.
     */
    FixedInterpolatorStringLookup(final StringLookup defaultStringLookup) {
        this(new HashMap<>(), defaultStringLookup, true);
    }

    /**
     * Gets the lookup map.
     *
     * @return The lookup map.
     */
    public Map<String, StringLookup> getStringLookupMap() {
        return stringLookupMap;
    }

    /**
     * Resolves the specified variable. This implementation will try to extract a variable prefix from the given
     * variable name (the first colon (':') is used as prefix separator). It then passes the name of the variable with
     * the prefix stripped to the lookup object registered for this prefix. If no prefix can be found or if the
     * associated lookup object cannot resolve this variable, the default lookup object will be used.
     *
     * @param var the name of the variable whose value is to be looked up
     * @return The value of this variable or <b>null</b> if it cannot be resolved
     */
    @Override
    public String lookup(String var) {
        if (var == null) {
            return null;
        }

        final int prefixPos = var.indexOf(PREFIX_SEPARATOR);
        if (prefixPos >= 0) {
            final String prefix = toKey(var.substring(0, prefixPos));
            final String name = var.substring(prefixPos + 1);
            final StringLookup lookup = stringLookupMap.get(prefix);
            String value = null;
            if (lookup != null) {
                value = lookup.lookup(name);
            }

            if (value != null) {
                return value;
            }
        }
        if (defaultStringLookup != null) {
            return defaultStringLookup.lookup(var);
        }
        return null;
    }

    @Override
    public String toString() {
        return super.toString() + " [stringLookupMap=" + stringLookupMap + ", defaultStringLookup="
            + defaultStringLookup + "]";
    }
}