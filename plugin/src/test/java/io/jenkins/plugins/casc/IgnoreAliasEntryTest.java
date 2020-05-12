package io.jenkins.plugins.casc;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class IgnoreAliasEntryTest {

    @Test
    public void aliasNull() {
        assertThat(ConfigurationAsCode.ignoreAliasEntry(null), is(false));
    }

    @Test
    public void aliasIgnoreXAlias() {
        assertThat(ConfigurationAsCode.ignoreAliasEntry("x-hello"), is(false));
    }

    @Test
    public void warnOnUnknownKey() {
        assertThat(ConfigurationAsCode.ignoreAliasEntry("bob"), is(true));
    }
}
