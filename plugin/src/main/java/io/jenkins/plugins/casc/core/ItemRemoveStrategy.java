package io.jenkins.plugins.casc.core;

import org.apache.commons.lang3.StringUtils;

public enum ItemRemoveStrategy {

    /**
     * Do not remove any items that are not present in the configuration.
     */
    NONE("none"),

    /**
     * Remove items that are not present in the configuration if they were
     * previously managed by Configuration as Code.
     */
    SYNC("sync"),

    /**
     * Remove all items that are not present in the configuration,
     * regardless of whether they were previously managed by Configuration as Code.
     */
    REMOVE_ALL("remove-all");

    private final String value;

    ItemRemoveStrategy(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    // In ItemRemoveStrategy.java
    public static ItemRemoveStrategy fromString(String strategy) {
        if (StringUtils.isBlank(strategy)) {
            return NONE;
        }
        String cleanStrategy = strategy.trim();
        for (ItemRemoveStrategy s : values()) {
            if (s.value.equalsIgnoreCase(cleanStrategy)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Invalid removeStrategy: " + strategy);
    }
}
