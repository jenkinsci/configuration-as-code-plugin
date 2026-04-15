package io.jenkins.plugins.casc.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import hudson.tasks.Builder;
import io.jenkins.plugins.casc.Configurator;
import io.jenkins.plugins.casc.impl.configurators.HeteroDescribableConfigurator;
import io.jenkins.plugins.casc.impl.configurators.PrimitiveConfigurator;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class DefaultConfiguratorRegistryTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    private DefaultConfiguratorRegistry registry;

    @Before
    public void setUp() {
        registry = new DefaultConfiguratorRegistry();
    }

    @SuppressWarnings("unused")
    public static class DummyTarget<T extends Builder> {
        public List<?> rawList;
        public List<List<String>> nestedList;
        public List<? extends Builder> wildcardList;
        public List<T> typeVarList;
        public List<?> unknownList;
    }

    public static class StringList extends ArrayList<String> {}

    private Type getTypeOf(String fieldName) throws NoSuchFieldException {
        return DummyTarget.class.getDeclaredField(fieldName).getGenericType();
    }

    @Test
    public void shouldSafelyHandleRawCollections() throws Exception {
        Type rawListType = getTypeOf("rawList");

        Configurator<?> configurator = registry.lookup(rawListType);

        assertNull("Raw collections should safely fall through and return null", configurator);
    }

    @Test
    public void shouldSafelyExtractNestedGenerics() throws Exception {
        Type nestedListType = getTypeOf("nestedList");

        Configurator<?> configurator = registry.lookup(nestedListType);

        assertNotNull("Configurator should be found for nested generic", configurator);
        assertTrue("Should resolve to PrimitiveConfigurator", configurator instanceof PrimitiveConfigurator);
        assertEquals("Target should resolve exactly to String.class", String.class, configurator.getTarget());
    }

    @Test
    public void shouldSafelyExtractWildcardUpperBounds() throws Exception {
        Type wildcardListType = getTypeOf("wildcardList");

        Configurator<?> configurator = registry.lookup(wildcardListType);

        assertNotNull("Configurator should be found for wildcard", configurator);
        assertTrue(
                "Should resolve to HeteroDescribableConfigurator",
                configurator instanceof HeteroDescribableConfigurator);
        assertEquals("Target should resolve exactly to Builder.class", Builder.class, configurator.getTarget());
    }

    @Test
    public void shouldSafelyExtractTypeVariables() throws Exception {
        Type typeVarListType = getTypeOf("typeVarList");

        Configurator<?> configurator = registry.lookup(typeVarListType);

        assertNotNull("Configurator should be found for TypeVariable", configurator);
        assertTrue(
                "Should resolve to HeteroDescribableConfigurator",
                configurator instanceof HeteroDescribableConfigurator);
        assertEquals("Target should resolve exactly to Builder.class", Builder.class, configurator.getTarget());
    }

    @Test
    public void shouldHandleCustomCollectionSubclass() {
        Configurator<?> configurator = registry.lookup(StringList.class);

        assertNotNull("Configurator should be found for custom collection subclass", configurator);
        assertTrue("Should resolve to PrimitiveConfigurator", configurator instanceof PrimitiveConfigurator);
        assertEquals("Target should resolve exactly to String.class", String.class, configurator.getTarget());
    }

    @Test
    public void shouldSafelyHandleUnboundedWildcards() throws Exception {
        Type unknownListType = getTypeOf("unknownList");

        Configurator<?> configurator = registry.lookup(unknownListType);

        assertNull("Unbounded wildcards resolve to Object and should safely return null", configurator);
    }
}
