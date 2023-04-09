package io.jenkins.plugins.casc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.junit.Test;

public class IgnoreAliasEntryTest {

    @Test
    public void aliasKeyIsNull() {
        assertThat(ConfigurationAsCode.isNotAliasEntry(null), is(false));
    }

    @Test
    public void aliasKeyStartsWithX() {
        assertThat(ConfigurationAsCode.isNotAliasEntry("x-hello"), is(false));
    }

    @Test
    public void UnknownRootElementShouldReturnTrue() {
        assertThat(ConfigurationAsCode.isNotAliasEntry("bob"), is(true));
    }
}
