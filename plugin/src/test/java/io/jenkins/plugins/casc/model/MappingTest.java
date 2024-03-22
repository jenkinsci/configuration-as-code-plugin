package io.jenkins.plugins.casc.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class MappingTest {

    @Test
    public void empty() {
        Mapping mapping = new Mapping();
        String aKey = "aKey";
        mapping.put(aKey, (CNode) null);
        Mapping clone = mapping.clone();
        assertNull(clone.get(aKey));
        assertNull(mapping.get(aKey));
    }

    @Test
    public void notEmpty() throws Exception {
        Mapping mapping = new Mapping();
        String aKey = "aKey";
        String aValue = "aValue";
        mapping.put(aKey, aValue);
        Mapping clone = mapping.clone();
        assertNotNull(clone.get(aKey));
        assertNotNull(mapping.get(aKey));
        assertEquals(aValue, clone.getScalarValue(aKey));
    }
}
