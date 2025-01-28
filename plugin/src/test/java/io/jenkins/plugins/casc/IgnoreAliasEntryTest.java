package io.jenkins.plugins.casc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.junit.jupiter.api.Test;

class IgnoreAliasEntryTest {

    @Test
    void aliasKeyIsNull() {
        assertThat(ConfigurationAsCode.isNotAliasEntry(null), is(false));
    }

    @Test
    void aliasKeyStartsWithX() {
        assertThat(ConfigurationAsCode.isNotAliasEntry("x-hello"), is(false));
    }

    @Test
    void UnknownRootElementShouldReturnTrue() {
        assertThat(ConfigurationAsCode.isNotAliasEntry("bob"), is(true));
    }
}
