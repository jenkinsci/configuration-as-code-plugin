package io.jenkins.plugins.casc.core;

import static io.jenkins.plugins.casc.core.ItemRemoveStrategy.DELETE_ALL;
import static io.jenkins.plugins.casc.core.ItemRemoveStrategy.DELETE_TRACKED;
import static io.jenkins.plugins.casc.core.ItemRemoveStrategy.KEEP;
import static io.jenkins.plugins.casc.core.ItemRemoveStrategy.fromString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class ItemRemoveStrategyTest {

    @Test
    public void testFromStringValidStrategies() {
        assertEquals(KEEP, fromString("keep"));
        assertEquals(DELETE_TRACKED, fromString("delete-tracked"));
        assertEquals(DELETE_ALL, fromString("delete-all"));
    }

    @Test
    public void testFromStringCaseInsensitive() {
        // Change these to use the new string values with mixed casing
        assertEquals(DELETE_TRACKED, fromString("Delete-Tracked"));
        assertEquals(DELETE_ALL, fromString("DELETE-ALL"));
        assertEquals(KEEP, fromString("kEeP"));
    }

    @Test
    public void testFromStringDefaultsToNoneOnBlankOrNull() {
        assertEquals(KEEP, fromString(null));
        assertEquals(KEEP, fromString(""));
        assertEquals(KEEP, fromString("   "));
    }

    @Test
    public void testFromStringThrowsOnUnknown() {
        assertThrows(IllegalArgumentException.class, () -> fromString("unknown-strategy"));
        assertThrows(IllegalArgumentException.class, () -> fromString("delete"));
        assertThrows(IllegalArgumentException.class, () -> fromString("garbage"));
    }

    @Test
    public void testGetValue() {
        assertEquals("keep", KEEP.getValue());
        assertEquals("delete-tracked", DELETE_TRACKED.getValue());
        assertEquals("delete-all", DELETE_ALL.getValue());
    }

    @Test
    public void testEnumImplicitMethods() {
        assertEquals(3, ItemRemoveStrategy.values().length);
        assertEquals(KEEP, ItemRemoveStrategy.valueOf("KEEP"));
        assertEquals(DELETE_TRACKED, ItemRemoveStrategy.valueOf("DELETE_TRACKED"));
        assertEquals(DELETE_ALL, ItemRemoveStrategy.valueOf("DELETE_ALL"));
    }
}
