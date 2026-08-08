package io.jenkins.plugins.casc.core;

import static io.jenkins.plugins.casc.core.ItemRemoveStrategy.NONE;
import static io.jenkins.plugins.casc.core.ItemRemoveStrategy.REMOVE_ALL;
import static io.jenkins.plugins.casc.core.ItemRemoveStrategy.SYNC;
import static io.jenkins.plugins.casc.core.ItemRemoveStrategy.fromString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class ItemRemoveStrategyTest {

    @Test
    public void testFromStringValidStrategies() {
        assertEquals(NONE, fromString("none"));
        assertEquals(SYNC, fromString("sync"));
        assertEquals(REMOVE_ALL, fromString("remove-all"));
    }

    @Test
    public void testFromStringCaseInsensitive() {
        assertEquals(SYNC, fromString("SYNC"));
        assertEquals(REMOVE_ALL, fromString("Remove-All"));
        assertEquals(NONE, fromString("NoNe"));
    }

    @Test
    public void testFromStringDefaultsToNoneOnBlankOrNull() {
        assertEquals(NONE, fromString(null));
        assertEquals(NONE, fromString(""));
        assertEquals(NONE, fromString("   "));
    }

    @Test
    public void testFromStringThrowsOnUnknown() {
        assertThrows(IllegalArgumentException.class, () -> fromString("unknown-strategy"));
        assertThrows(IllegalArgumentException.class, () -> fromString("delete"));
        assertThrows(IllegalArgumentException.class, () -> fromString("garbage"));
    }
}
