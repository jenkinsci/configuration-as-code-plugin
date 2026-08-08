package io.jenkins.plugins.casc.core;

import org.apache.commons.lang3.StringUtils;

public enum ItemRemoveStrategy {
    NONE("none"),
    SYNC("sync"),
    REMOVE_ALL("remove-all");

    private final String value;

    ItemRemoveStrategy(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ItemRemoveStrategy fromString(String strategy) {
        if (StringUtils.isBlank(strategy)) {
            return NONE;
        }
        for (ItemRemoveStrategy s : values()) {
            if (s.value.equalsIgnoreCase(strategy)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Invalid removeStrategy: " + strategy);
    }
}
