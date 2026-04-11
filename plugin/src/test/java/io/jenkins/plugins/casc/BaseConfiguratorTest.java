package io.jenkins.plugins.casc;

import static org.junit.Assert.assertEquals;

import io.jenkins.plugins.casc.model.Mapping;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;

public class BaseConfiguratorTest {

    public static class Animal {}

    public static class Dog extends Animal {}

    public static class Cat extends Animal {}

    public static class Vehicle {}

    public static class Car extends Vehicle {}

    @SuppressWarnings("unused")
    public static class DummyTarget {

        public String getStandard() {
            return null;
        }

        public void setStandard(String val) {}

        public Car getRide() {
            return null;
        }

        public void setRide(Vehicle val) {}

        public void setRide(Car val) {}

        public void setRide(Object val) {}

        public void setRide() {}

        public Animal getPet() {
            return null;
        }

        public void setPet(Object pet) {}

        public void setPet(Animal pet) {}

        public Animal getAmbiguous() {
            return null;
        }

        public void setAmbiguous(Dog dog) {}

        public void setAmbiguous(Cat cat) {}
    }

    public static class DummyConfigurator extends BaseConfigurator<DummyTarget> {
        @Override
        public Class<DummyTarget> getTarget() {
            return DummyTarget.class;
        }

        @Override
        protected DummyTarget instance(Mapping mapping, ConfigurationContext context) {
            return new DummyTarget();
        }
    }

    @Test
    public void testDescribeResolvesBestSetters() {
        DummyConfigurator configurator = new DummyConfigurator();
        Set<Attribute<DummyTarget, ?>> attributes = configurator.describe();

        Map<String, Class<?>> resolvedAttributes =
                attributes.stream().collect(Collectors.toMap(Attribute::getName, attr -> (Class<?>) attr.getType()));

        assertEquals("Should discover exactly 4 configurable properties", 4, resolvedAttributes.size());

        assertEquals("Standard setter should resolve to String", String.class, resolvedAttributes.get("standard"));

        assertEquals(
                "Exact match should win over subclasses and superclasses", Car.class, resolvedAttributes.get("ride"));

        assertEquals(
                "Most specific compatible type should win over Object", Animal.class, resolvedAttributes.get("pet"));

        assertEquals(
                "Alphabetical fallback should deterministically choose Cat over Dog",
                Cat.class,
                resolvedAttributes.get("ambiguous"));

        assertEquals(Set.of("standard", "ride", "pet", "ambiguous"), resolvedAttributes.keySet());
    }
}
