package io.jenkins.plugins.casc.impl.configurators;

import static org.junit.Assert.assertEquals;

import hudson.model.Describable;
import hudson.model.Descriptor;
import java.util.Arrays;
import java.util.Collections;
import org.jenkinsci.Symbol;
import org.junit.Test;

public class DescriptorConfiguratorTest {

    public static class DummyTask implements Describable<DummyTask> {
        @Override
        public Descriptor<DummyTask> getDescriptor() {
            throw new UnsupportedOperationException("Not required for name extraction tests");
        }
    }

    public static class ParameterizedTask<T> implements Describable<ParameterizedTask<T>> {
        @Override
        public Descriptor<ParameterizedTask<T>> getDescriptor() {
            throw new UnsupportedOperationException();
        }
    }

    public static class DummyTaskDescriptor extends Descriptor<DummyTask> {
        public DummyTaskDescriptor() {
            super(DummyTask.class);
        }
    }

    @Symbol({"primary", "alias"})
    public static class MultiSymbolDescriptor extends Descriptor<DummyTask> {
        public MultiSymbolDescriptor() {
            super(DummyTask.class);
        }
    }

    public abstract static class SimulatedBuildStepDescriptor<T extends Describable<T>> extends Descriptor<T> {
        protected SimulatedBuildStepDescriptor(Class<? extends T> clazz) {
            super(clazz);
        }
    }

    public static class DeepTaskDescriptor extends SimulatedBuildStepDescriptor<DummyTask> {
        public DeepTaskDescriptor() {
            super(DummyTask.class);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static class ParameterizedDescriptor extends Descriptor<ParameterizedTask<String>> {
        public ParameterizedDescriptor() {
            super((Class) ParameterizedTask.class);
        }
    }

    @SuppressWarnings("rawtypes")
    public static class RawDescriptor extends Descriptor {
        @SuppressWarnings("unchecked")
        public RawDescriptor(Class clazz) {
            super(clazz);
        }
    }

    @Test
    public void testMultipleSymbols() {
        DescriptorConfigurator configurator = new DescriptorConfigurator(new MultiSymbolDescriptor());

        assertEquals("Should use the first symbol as primary name", "primary", configurator.getName());
        assertEquals(
                "Should return all symbols as aliases", Arrays.asList("primary", "alias"), configurator.getNames());
    }

    @Test
    public void testNameResolvedFromGenericExtraction() {
        DescriptorConfigurator configurator = new DescriptorConfigurator(new DummyTaskDescriptor());

        assertEquals("Should extract 'DummyTask' and convert to camelCase", "dummyTask", configurator.getName());
        assertEquals(Collections.singletonList("dummyTask"), configurator.getNames());
    }

    @Test
    public void testNameResolvedFromParameterizedType() {
        DescriptorConfigurator configurator = new DescriptorConfigurator(new ParameterizedDescriptor());

        assertEquals(
                "Should unwrap ParameterizedType to its raw class name", "parameterizedTask", configurator.getName());
    }

    @Test
    public void testNameResolvedFromDeepInheritance() {
        DescriptorConfigurator configurator = new DescriptorConfigurator(new DeepTaskDescriptor());

        assertEquals(
                "Should bypass type erasure and extract from superclass hierarchy",
                "dummyTask",
                configurator.getName());
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void testFallbackWithAnonymousTargetClass() {

        Class<?> anonymousTarget = new DummyTask() {}.getClass();
        Descriptor rawDescriptor = new RawDescriptor(anonymousTarget);
        DescriptorConfigurator configurator = new DescriptorConfigurator(rawDescriptor);

        assertEquals(
                "Should unwrap anonymous target class via the fallback logic", "dummyTask", configurator.getName());
    }
}
