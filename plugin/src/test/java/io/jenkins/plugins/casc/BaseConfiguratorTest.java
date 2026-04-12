package io.jenkins.plugins.casc;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.jenkins.plugins.casc.model.Mapping;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
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

    public static class FakeSecret {}

    public static class A_Shape {}

    public static class B_Polygon extends A_Shape {}

    public static class C_Square extends B_Polygon {}

    public interface Z_Interface {}

    public static class A_Concrete {}

    public static class V_Class {}

    public static class W_Class {}

    public static class X_Class {}

    public static class Y_Class {}

    public static class Z_Class {}

    @SuppressWarnings("unused")
    public static class DummyTarget {

        private String[] items;

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

        public void setWriteOnly(String val) {}

        public FakeSecret getMismatchedToken() {
            return null;
        }

        public void setMismatchedToken(String val) {}

        public String[] getItems() {
            return items;
        }

        public void setItems(String[] items) {
            this.items = items;
        }

        public List<String> getArrayFallback() {
            return null;
        }

        public void setArrayFallback(String val) {}

        public void setArrayFallback(String[] val) {}

        public FakeSecret getShapeSubtype() {
            return null;
        }

        public void setShapeSubtype(A_Shape val) {}

        public void setShapeSubtype(B_Polygon val) {}

        public void setShapeSubtype(C_Square val) {}

        public Object getConcreteWins() {
            return null;
        }

        public void setConcreteWins(Z_Interface val) {}

        public void setConcreteWins(A_Concrete val) {}

        public Object getReverseAlphabetical() {
            return null;
        }

        public void setReverseAlphabetical(Z_Class val) {}

        public void setReverseAlphabetical(Y_Class val) {}

        public void setReverseAlphabetical(X_Class val) {}

        public void setReverseAlphabetical(W_Class val) {}

        public void setReverseAlphabetical(V_Class val) {}

        public Integer getPrimitiveSetter() {
            return null;
        }

        public void setPrimitiveSetter(int val) {}

        public int getWrapperSetter() {
            return 0;
        }

        public void setWrapperSetter(Integer val) {}

        public Boolean getPrimitiveBoolean() {
            return null;
        }

        public void setPrimitiveBoolean(boolean val) {}

        public Long getPrimitiveLong() {
            return null;
        }

        public void setPrimitiveLong(long val) {}

        public Double getPrimitiveDouble() {
            return null;
        }

        public void setPrimitiveDouble(double val) {}

        public Float getPrimitiveFloat() {
            return null;
        }

        public void setPrimitiveFloat(float val) {}

        public Byte getPrimitiveByte() {
            return null;
        }

        public void setPrimitiveByte(byte val) {}

        public Character getPrimitiveChar() {
            return null;
        }

        public void setPrimitiveChar(char val) {}

        public Short getPrimitiveShort() {
            return null;
        }

        public void setPrimitiveShort(short val) {}

        public void getVoidEdgeCase() {}

        public void setVoidEdgeCase(String val) {}
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

    @SuppressWarnings("unused")
    public static class NoGetterTarget {
        public void setCompletelyMissing(String val) {}

        public void setRequiresParam(String val) {}

        public String getRequiresParam(String param) {
            return param;
        }

        public void setFakeBoolean(String val) {}

        public String isFakeBoolean() {
            return "I am a String, not a boolean";
        }
    }

    public static class NoGetterConfigurator extends BaseConfigurator<NoGetterTarget> {
        @Override
        public Class<NoGetterTarget> getTarget() {
            return NoGetterTarget.class;
        }

        @Override
        protected NoGetterTarget instance(Mapping mapping, ConfigurationContext context) {
            return new NoGetterTarget();
        }
    }

    @Test
    public void testDescribeResolvesBestSetters() {
        DummyConfigurator configurator = new DummyConfigurator();
        Set<Attribute<DummyTarget, ?>> attributes = configurator.describe();

        Map<String, Class<?>> resolvedAttributes =
                attributes.stream().collect(Collectors.toMap(Attribute::getName, attr -> (Class<?>) attr.getType()));

        assertEquals("Should discover exactly 20 configurable properties", 20, resolvedAttributes.size());

        assertEquals("Standard setter should resolve to String", String.class, resolvedAttributes.get("standard"));

        assertEquals(
                "Exact match should win over subclasses and superclasses", Car.class, resolvedAttributes.get("ride"));

        assertEquals(
                "Most specific compatible type should win over Object", Animal.class, resolvedAttributes.get("pet"));

        assertEquals(
                "Alphabetical fallback should deterministically choose Cat over Dog",
                Cat.class,
                resolvedAttributes.get("ambiguous"));

        assertEquals(
                "Disjoint getter/setter types should fallback to the available setter parameter type",
                String.class,
                resolvedAttributes.get("mismatchedToken"));

        assertEquals(
                "Array types should be resolved to their component type",
                String.class,
                resolvedAttributes.get("items"));

        assertEquals(
                "When no exact match exists between getters and setters, array setters should be preferred",
                String.class,
                resolvedAttributes.get("arrayFallback"));

        assertEquals(
                "When multiple setters exist in an inheritance hierarchy, the most specific subtype should win",
                C_Square.class,
                resolvedAttributes.get("shapeSubtype"));

        assertEquals(
                "Concrete class should win over interface when both are candidates",
                A_Concrete.class,
                resolvedAttributes.get("concreteWins"));

        assertEquals(
                "Alphabetical fallback should resolve to the alphabetically first class",
                V_Class.class,
                resolvedAttributes.get("reverseAlphabetical"));

        assertEquals(
                "Should map primitive setter with wrapper getter",
                int.class,
                resolvedAttributes.get("primitiveSetter"));

        assertEquals(
                "Should map wrapper setter with primitive getter",
                Integer.class,
                resolvedAttributes.get("wrapperSetter"));

        assertEquals(boolean.class, resolvedAttributes.get("primitiveBoolean"));
        assertEquals(long.class, resolvedAttributes.get("primitiveLong"));
        assertEquals(double.class, resolvedAttributes.get("primitiveDouble"));
        assertEquals(float.class, resolvedAttributes.get("primitiveFloat"));
        assertEquals(byte.class, resolvedAttributes.get("primitiveByte"));
        assertEquals(char.class, resolvedAttributes.get("primitiveChar"));
        assertEquals(short.class, resolvedAttributes.get("primitiveShort"));
        assertEquals(String.class, resolvedAttributes.get("voidEdgeCase"));

        Attribute<DummyTarget, ?> itemsAttr = attributes.stream()
                .filter(a -> a.getName().equals("items"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("items attribute not found"));
        assertTrue("items attribute should be marked as multiple", itemsAttr.isMultiple());

        assertEquals(
                Set.of(
                        "standard",
                        "ride",
                        "pet",
                        "ambiguous",
                        "mismatchedToken",
                        "items",
                        "arrayFallback",
                        "shapeSubtype",
                        "concreteWins",
                        "reverseAlphabetical",
                        "primitiveSetter",
                        "wrapperSetter",
                        "primitiveBoolean",
                        "primitiveLong",
                        "primitiveDouble",
                        "primitiveFloat",
                        "primitiveByte",
                        "primitiveChar",
                        "primitiveShort",
                        "voidEdgeCase"),
                resolvedAttributes.keySet());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void testCollectionToArrayConversion() throws Exception {
        DummyConfigurator configurator = new DummyConfigurator();
        Set<Attribute<DummyTarget, ?>> attributes = configurator.describe();

        Attribute<DummyTarget, ?> itemsAttr = attributes.stream()
                .filter(a -> a.getName().equals("items"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("items attribute not found"));

        DummyTarget target = new DummyTarget();

        List<String> inputCollection = Arrays.asList("foo", "bar", "baz");

        ((Attribute) itemsAttr).setValue(target, inputCollection);

        String[] result = target.getItems();
        assertNotNull("Array should have been set", result);
        assertEquals("Array should have the same size as the collection", 3, result.length);
        assertArrayEquals(new String[] {"foo", "bar", "baz"}, result);
    }

    @Test
    public void testFindGetterReturnsNullForMissingOrInvalidGetters() {
        NoGetterConfigurator configurator = new NoGetterConfigurator();

        Set<Attribute<NoGetterTarget, ?>> attributes = configurator.describe();

        assertTrue("Properties without valid getters should yield no attributes", attributes.isEmpty());
    }

    @Test
    public void testResolveBestSetterBranchCoverage() throws Exception {
        DummyConfigurator configurator = new DummyConfigurator();

        Method resolveMethod = BaseConfigurator.class.getDeclaredMethod("resolveBestSetter", List.class, Class.class);
        resolveMethod.setAccessible(true);

        Method setObj = DummyTarget.class.getMethod("setPet", Object.class);
        Method setAnimal = DummyTarget.class.getMethod("setPet", Animal.class);

        List<Method> orderedMethods = Arrays.asList(setObj, setAnimal);

        Method best = (Method) resolveMethod.invoke(configurator, orderedMethods, null);

        assertEquals("Should upgrade bestType and resolve to the more specific Animal setter", setAnimal, best);
    }
}
