package io.jenkins.plugins.casc.impl.configurators;

import static io.jenkins.plugins.casc.misc.Util.assertLogContains;
import static io.jenkins.plugins.casc.misc.Util.assertNotInLog;
import static io.jenkins.plugins.casc.misc.Util.getJenkinsRoot;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.kohsuke.stapler.Stapler.CONVERT_UTILS;

import hudson.util.Secret;
import io.jenkins.plugins.casc.ConfigurationAsCode;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.Configurator;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.impl.configurators.nonnull.ClassParametersAreNonnullByDefault;
import io.jenkins.plugins.casc.impl.configurators.nonnull.NonnullParameterConstructor;
import io.jenkins.plugins.casc.impl.configurators.nonnull.nonnullparampackage.PackageParametersAreNonnullByDefault;
import io.jenkins.plugins.casc.impl.configurators.nonnull.nonnullparampackage.PackageParametersNonNullCheckForNull;
import io.jenkins.plugins.casc.misc.Util;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import io.jenkins.plugins.casc.model.Scalar;
import io.jenkins.plugins.casc.model.Sequence;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.PostConstruct;
import org.apache.commons.beanutils.Converter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.LogRecorder;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.Stapler;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@WithJenkins
class DataBoundConfiguratorTest {

    private JenkinsRule j;

    private final LogRecorder logging = new LogRecorder();

    /** captured exception to be printed after Jenkins is shut down */
    private static Throwable savedConfiguratorException;

    @BeforeEach
    void tearUp(JenkinsRule j) {
        this.j = j;
        logging.record(Logger.getLogger(DataBoundConfigurator.class.getName()), Level.FINEST)
                .capture(2048);
    }

    @Test
    void configure_databound() throws Exception {
        Mapping config = new Mapping();
        config.put("foo", "foo");
        config.put("bar", "true");
        config.put("qix", "123");
        config.put("zot", "DataBoundSetter");
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        final Foo configured =
                (Foo) registry.lookupOrFail(Foo.class).configure(config, new ConfigurationContext(registry));
        assertEquals("foo", configured.foo);
        assertTrue(configured.bar);
        assertEquals(123, configured.qix);
        assertEquals("DataBoundSetter", configured.zot);
        assertThat(configured.initialized, is(true));
    }

    @Test
    void exportYaml() throws Exception {
        Foo foo = new Foo("foo", true, 42);
        foo.setZot("zot");
        foo.setDbl(12.34);
        foo.setFlt(1f); // whole numbers are exported as "<number>.0"
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        final Configurator c = registry.lookupOrFail(Foo.class);
        final ConfigurationContext context = new ConfigurationContext(registry);
        final CNode node = c.describe(foo, context);
        assertNotNull(node);
        assertInstanceOf(Mapping.class, node);
        Mapping map = (Mapping) node;
        assertEquals("foo", map.get("foo").toString());
        assertEquals("true", map.get("bar").toString());
        assertEquals("42", map.get("qix").toString());
        assertEquals("zot", map.get("zot").toString());
        assertEquals("12.34", map.get("dbl").toString());
        assertEquals("1.0", map.get("flt").toString());
        assertEquals("\"foo\"", Util.toYamlString(map.get("foo")).trim());
        assertEquals("true", Util.toYamlString(map.get("bar")).trim());
        assertEquals("42", Util.toYamlString(map.get("qix")).trim());
        assertEquals("\"zot\"", Util.toYamlString(map.get("zot")).trim());
        assertEquals("\"12.34\"", Util.toYamlString(map.get("dbl")).trim());
        assertEquals("\"1.0\"", Util.toYamlString(map.get("flt")).trim());
        assertFalse(map.containsKey("other"));
    }

    @Test
    void configureWithSets() throws Exception {
        Mapping config = new Mapping();
        Sequence sequence = new Sequence();
        sequence.add(new Scalar("bar"));
        sequence.add(new Scalar("foo"));
        config.put("strings", sequence);
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        final Bar configured =
                (Bar) registry.lookupOrFail(Bar.class).configure(config, new ConfigurationContext(registry));
        Set<String> strings = configured.getStrings();
        assertTrue(strings.contains("foo"));
        assertTrue(strings.contains("bar"));
        assertFalse(strings.contains("baz"));
    }

    @Test
    void configureWithEmptySet() throws Exception {
        Mapping config = new Mapping();
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        final Bar configured =
                (Bar) registry.lookupOrFail(Bar.class).configure(config, new ConfigurationContext(registry));
        Set<String> strings = configured.getStrings();
        assertEquals(0, strings.size());
    }

    @Test
    void nonnullConstructorParameter() throws Exception {
        Mapping config = new Mapping();
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        final NonnullParameterConstructor configured =
                (NonnullParameterConstructor) registry.lookupOrFail(NonnullParameterConstructor.class)
                        .configure(config, new ConfigurationContext(registry));
        assertEquals(0, configured.getStrings().size());
    }

    @Test
    void classParametersAreNonnullByDefault() throws Exception {
        Mapping config = new Mapping();
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        final ClassParametersAreNonnullByDefault configured =
                (ClassParametersAreNonnullByDefault) registry.lookupOrFail(ClassParametersAreNonnullByDefault.class)
                        .configure(config, new ConfigurationContext(registry));
        assertTrue(configured.getStrings().isEmpty());
    }

    @Test
    void packageParametersAreNonnullByDefault() {
        Mapping config = new Mapping();
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();

        String expectedMessage =
                "string is required to configure class io.jenkins.plugins.casc.impl.configurators.nonnull.nonnullparampackage.PackageParametersAreNonnullByDefault";

        ConfiguratorException exception = assertThrows(
                ConfiguratorException.class, () -> registry.lookupOrFail(PackageParametersAreNonnullByDefault.class)
                        .configure(config, new ConfigurationContext(registry)));

        assertThat(exception.getMessage(), is(expectedMessage));

        savedConfiguratorException = exception;
    }

    @Test
    @Issue("#1025")
    void packageParametersAreNonnullByDefaultButCanBeNullable() throws Exception {
        Mapping config = new Mapping();
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        final PackageParametersNonNullCheckForNull configured =
                (PackageParametersNonNullCheckForNull) registry.lookupOrFail(PackageParametersNonNullCheckForNull.class)
                        .configure(config, new ConfigurationContext(registry));
        assertNull(configured.getSecret());
    }

    @Test
    @SuppressWarnings("unchecked")
    void exportWithSets() throws Exception {
        HashSet<String> set = new HashSet<>();
        set.add("foo");

        Bar bar = new Bar(set);
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        final Configurator c = registry.lookupOrFail(Bar.class);
        final ConfigurationContext context = new ConfigurationContext(registry);
        CNode node = c.describe(bar, context);
        assertNotNull(node);
        assertInstanceOf(Mapping.class, node);
        Mapping map = (Mapping) node;
        assertEquals("[foo]", map.get("strings").toString());
        assertEquals("- \"foo\"", Util.toYamlString(map.get("strings")).trim());
        assertFalse(map.containsKey("other"));

        // now with two elements
        set.add("bar");
        node = c.describe(bar, context);
        assertNotNull(node);
        assertInstanceOf(Mapping.class, node);
        map = (Mapping) node;
        assertEquals("[bar, foo]", map.get("strings").toString());
        assertEquals(
                "- \"bar\"\n- \"foo\"", Util.toYamlString(map.get("strings")).trim());
    }

    @Test
    @Issue("PR #838, Issue #222")
    void export_mapping_should_not_be_null() throws Exception {
        j.createFreeStyleProject("testJob1");
        ConfigurationAsCode casc = ConfigurationAsCode.get();
        casc.configure(
                this.getClass().getResource("DataBoundDescriptorNonNull.yml").toString());

        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        final Mapping configNode = getJenkinsRoot(context);
        final CNode viewsNode = configNode.get("views");
        Mapping listView = viewsNode.asSequence().get(1).asMapping().get("list").asMapping();
        Mapping otherListView =
                viewsNode.asSequence().get(2).asMapping().get("list").asMapping();
        Sequence listViewColumns = listView.get("columns").asSequence();
        Sequence otherListViewColumns = otherListView.get("columns").asSequence();
        assertNotNull(listViewColumns);
        assertEquals(6, listViewColumns.size());
        assertNotNull(otherListViewColumns);
        assertEquals(7, otherListViewColumns.size());
        assertEquals("loggedInUsersCanDoAnything", configNode.getScalarValue("authorizationStrategy"));
        assertEquals("plainText", configNode.getScalarValue("markupFormatter"));
    }

    @Test
    void shouldThrowConfiguratorException() {
        Mapping config = new Mapping();
        config.put("foo", "foo");
        config.put("bar", "abcd");
        config.put("qix", "99");
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfiguratorException e = assertThrows(
                ConfiguratorException.class,
                () -> registry.lookupOrFail(Foo.class).configure(config, new ConfigurationContext(registry)),
                "action is excepted to throw ConfiguratorException!");
        assertThat(
                e.getMessage(),
                is(
                        "foo: Failed to construct instance of class io.jenkins.plugins.casc.impl.configurators.DataBoundConfiguratorTest$Foo.\n"
                                + " Constructor: public io.jenkins.plugins.casc.impl.configurators.DataBoundConfiguratorTest$Foo(java.lang.String,boolean,int).\n"
                                + " Arguments: [java.lang.String, java.lang.Boolean, java.lang.Integer].\n"
                                + " Expected Parameters: foo java.lang.String, bar boolean, qix int"));

        savedConfiguratorException = e;
    }

    @AfterAll
    static void checkConfiguratorExceptionCanBeReportedWithoutJenkins() {
        if (savedConfiguratorException != null) {
            StringWriter sw = new StringWriter();
            try (PrintWriter pw = new PrintWriter(sw)) {
                savedConfiguratorException.printStackTrace(pw);
            }
            savedConfiguratorException = null;
        }
    }

    @Test
    void shouldNotLogSecrets() throws Exception {
        Mapping config = new Mapping();
        config.put("secret", "mySecretValue");
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        registry.lookupOrFail(SecretHolder.class).configure(config, new ConfigurationContext(registry));
        assertLogContains(logging, "secret");
        assertNotInLog(logging, "mySecretValue");
    }

    @Test
    @Issue("SECURITY-1497")
    void shouldNotLogSecretsForUndefinedConstructors() throws Exception {
        Mapping config = new Mapping();
        config.put("secret", "mySecretValue");
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        registry.lookupOrFail(SecretHolderWithString.class).configure(config, new ConfigurationContext(registry));
        assertLogContains(logging, "secret");
        assertNotInLog(logging, "mySecretValue");
    }

    @Test
    void shouldExportArray() throws Exception {
        ArrayConstructor obj = new ArrayConstructor(new Foo[] {new Foo("", false, 0)});

        ConfiguratorRegistry registry = ConfiguratorRegistry.get();

        final Configurator c = registry.lookupOrFail(ArrayConstructor.class);
        final ConfigurationContext context = new ConfigurationContext(registry);
        CNode node = c.describe(obj, context);

        assertNotNull(node);
        assertInstanceOf(Mapping.class, node);
        Mapping map = (Mapping) node;
        assertEquals("[{qix=0, bar=false, foo=}]", map.get("anArray").toString());
    }

    public static class Foo {

        final String foo;
        final boolean bar;
        final int qix;
        String zot;
        String other;
        double dbl;
        private float flt;
        boolean initialized;

        @DataBoundConstructor
        public Foo(String foo, boolean bar, int qix) {
            this.foo = foo;
            this.bar = bar;
            this.qix = qix;
            if (qix == 99) {
                throw new IllegalArgumentException("Magic test fail");
            }
        }

        @DataBoundSetter
        public void setZot(String zot) {
            this.zot = zot;
        }

        @DataBoundSetter
        public void setOther(String other) {
            this.other = other;
        }

        @DataBoundSetter
        public void setDbl(double dbl) {
            this.dbl = dbl;
        }

        @PostConstruct
        public void init() {
            this.initialized = true;
        }

        public String getFoo() {
            return foo;
        }

        public boolean isBar() {
            return bar;
        }

        public int getQix() {
            return qix;
        }

        public String getZot() {
            return zot;
        }

        public String getOther() {
            return other;
        }

        public double getDbl() {
            return dbl;
        }

        public float getFlt() {
            return flt;
        }

        @DataBoundSetter
        public void setFlt(float flt) {
            this.flt = flt;
        }
    }

    public static class Bar {
        final Set<String> strings;

        @DataBoundConstructor
        @ParametersAreNonnullByDefault
        public Bar(Set<String> strings) {
            this.strings = strings;
        }

        public Set<String> getStrings() {
            return strings;
        }
    }

    public static class SecretHolder {

        Secret secret;

        @DataBoundConstructor
        public SecretHolder(Secret secret) {
            this.secret = secret;
        }
    }

    public static class SecretHolderWithString {

        Secret secret;

        @DataBoundConstructor
        public SecretHolderWithString(String secret) {
            this.secret = Secret.fromString(secret);
        }
    }

    public static class ArrayConstructor {
        private final Foo[] anArray;

        @DataBoundConstructor
        public ArrayConstructor(Foo[] anArray) {
            this.anArray = anArray;
        }
    }

    @SuppressWarnings("ClassCanBeRecord")
    public static class CustomItem {
        private final String value;

        @DataBoundConstructor
        public CustomItem(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    @SuppressWarnings("ClassCanBeRecord")
    public static class CustomItemListHolder {
        private final List<CustomItem> items;

        @DataBoundConstructor
        public CustomItemListHolder(List<CustomItem> items) {
            this.items = items;
        }

        public List<CustomItem> getItems() {
            return items;
        }
    }

    @Test
    @Issue("https://github.com/jenkinsci/configuration-as-code-plugin/issues/2346")
    void exportWithCustomConverterIteratesOverList() throws Exception {
        List<CustomItem> list = Arrays.asList(new CustomItem("A"), new CustomItem("B"));
        CustomItemListHolder holder = new CustomItemListHolder(list);

        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        final Configurator<Object> c = registry.lookupOrFail(CustomItemListHolder.class);
        final ConfigurationContext context = new ConfigurationContext(registry);

        CNode node = c.describe(holder, context);

        assertNotNull(node);
        assertInstanceOf(Mapping.class, node);
        Mapping map = (Mapping) node;

        assertEquals(
                "- value: \"A\"\n- value: \"B\"",
                Util.toYamlString(map.get("items")).trim());
    }

    @SuppressWarnings("ClassCanBeRecord")
    public static class CustomItemSetHolder {
        private final Set<CustomItem> items;

        @DataBoundConstructor
        public CustomItemSetHolder(Set<CustomItem> items) {
            this.items = items;
        }

        public Set<CustomItem> getItems() {
            return items;
        }
    }

    @Test
    void exportWithCustomConverterIteratesOverSet() throws Exception {
        Set<CustomItem> set = new HashSet<>();
        set.add(new CustomItem("A"));
        set.add(new CustomItem("B"));

        CustomItemSetHolder holder = new CustomItemSetHolder(set);

        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        Configurator<Object> c = registry.lookupOrFail(CustomItemSetHolder.class);

        CNode node = c.describe(holder, new ConfigurationContext(registry));
        Mapping map = (Mapping) node;

        String yaml =
                Util.toYamlString(Objects.requireNonNull(map).get("items")).trim();

        assertTrue(yaml.contains("value: \"A\""));
        assertTrue(yaml.contains("value: \"B\""));
    }

    @Test
    void exportWithSingleElementList() throws Exception {
        CustomItemListHolder holder = new CustomItemListHolder(List.of(new CustomItem("A")));

        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        Configurator<Object> c = registry.lookupOrFail(CustomItemListHolder.class);

        CNode node = c.describe(holder, new ConfigurationContext(registry));
        Mapping map = (Mapping) node;

        assertEquals(
                "- value: \"A\"",
                Util.toYamlString(Objects.requireNonNull(map).get("items")).trim());
    }

    @Test
    void exportWithCustomConverterIteratesOverListWithNull() throws Exception {
        List<CustomItem> list = Arrays.asList(new CustomItem("A"), null);
        CustomItemListHolder holder = new CustomItemListHolder(list);

        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        Configurator<Object> c = registry.lookupOrFail(CustomItemListHolder.class);

        CNode node = c.describe(holder, new ConfigurationContext(registry));

        assertNotNull(node, "Node should not be null");
        assertInstanceOf(Mapping.class, node, "Node should be exported as a Mapping");

        String yaml = Util.toYamlString(node);
        assertTrue(yaml.contains("A"), "The valid item 'A' should be present in the exported YAML");
    }

    @Test
    void configureIteratesOverList() {
        Mapping config = new Mapping();
        Sequence items = new Sequence();

        Mapping itemA = new Mapping();
        itemA.put("value", "A");
        items.add(itemA);

        Mapping itemB = new Mapping();
        itemB.put("value", "B");
        items.add(itemB);

        config.put("items", items);

        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        CustomItemListHolder configured = (CustomItemListHolder)
                registry.lookupOrFail(CustomItemListHolder.class).configure(config, new ConfigurationContext(registry));

        assertNotNull(configured);
        assertEquals(2, configured.getItems().size());
        assertEquals("A", configured.getItems().get(0).getValue());
        assertEquals("B", configured.getItems().get(1).getValue());
    }

    @Test
    void configureConvertsListToSet() {
        Mapping config = new Mapping();
        Sequence items = new Sequence();

        Mapping itemA = new Mapping();
        itemA.put("value", "A");
        items.add(itemA);

        Mapping itemB = new Mapping();
        itemB.put("value", "B");
        items.add(itemB);

        config.put("items", items);

        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        CustomItemSetHolder configured = (CustomItemSetHolder)
                registry.lookupOrFail(CustomItemSetHolder.class).configure(config, new ConfigurationContext(registry));

        assertNotNull(configured);
        assertEquals(2, configured.getItems().size());
        assertNotNull(configured.getItems());

        assertTrue(configured.getItems().stream().anyMatch(i -> "A".equals(i.getValue())));
        assertTrue(configured.getItems().stream().anyMatch(i -> "B".equals(i.getValue())));
    }

    @SuppressWarnings("ClassCanBeRecord")
    public static class StaplerOnlyItem {
        private final String value;

        public StaplerOnlyItem(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    @SuppressWarnings("ClassCanBeRecord")
    public static class StaplerOnlyItemListHolder {
        private final List<StaplerOnlyItem> items;

        @DataBoundConstructor
        public StaplerOnlyItemListHolder(List<StaplerOnlyItem> items) {
            this.items = items;
        }

        public List<StaplerOnlyItem> getItems() {
            return items;
        }
    }

    public static class StaplerOnlyItemConverter implements Converter {
        @Override
        public <T> T convert(Class<T> type, Object value) {
            assertFalse(value instanceof List, "Converter must be called per item, not per list");
            if (value == null) {
                return null;
            }

            return type.cast(new StaplerOnlyItem("converted-by-stapler-" + value));
        }
    }

    @Test
    void shouldConvertListItemsUsingStaplerConverter() {
        CONVERT_UTILS.register(new StaplerOnlyItemConverter(), StaplerOnlyItem.class);

        try {
            Mapping config = new Mapping();
            Sequence items = new Sequence();

            items.add(new Scalar("ItemA"));
            items.add(new Scalar("ItemB"));
            config.put("items", items);

            ConfiguratorRegistry registry = ConfiguratorRegistry.get();
            StaplerOnlyItemListHolder configured =
                    (StaplerOnlyItemListHolder) registry.lookupOrFail(StaplerOnlyItemListHolder.class)
                            .configure(config, new ConfigurationContext(registry));

            assertNotNull(configured);
            assertEquals(2, configured.getItems().size());
            assertEquals(
                    "converted-by-stapler-ItemA", configured.getItems().get(0).getValue());
            assertEquals(
                    "converted-by-stapler-ItemB", configured.getItems().get(1).getValue());

        } finally {
            CONVERT_UTILS.deregister(StaplerOnlyItem.class);
        }
    }

    public static class ListGetterSetCtor {

        @DataBoundConstructor
        @SuppressWarnings("unused")
        public ListGetterSetCtor(Set<String> items) {
            // intentionally unused: required to exercise Set to Collection conversion
        }

        @SuppressWarnings("unused")
        public List<String> getItems() {
            return List.of("A", "B");
        }
    }

    @Test
    void describe_convertsCollectionToSetForConstructor() throws Exception {
        ListGetterSetCtor obj = new ListGetterSetCtor(Set.of("A", "B"));

        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        CNode node = registry.lookupOrFail(ListGetterSetCtor.class).describe(obj, new ConfigurationContext(registry));

        assertNotNull(node);
        assertInstanceOf(Mapping.class, node);

        Mapping mapping = (Mapping) node;
        assertEquals(2, mapping.get("items").asSequence().size());
    }

    @SuppressWarnings("ClassCanBeRecord")
    public static class CollectionCtorArrayGetter {
        private final List<StaplerOnlyItem> items;

        @DataBoundConstructor
        public CollectionCtorArrayGetter(List<StaplerOnlyItem> items) {
            this.items = items;
        }

        @SuppressWarnings("unused")
        public StaplerOnlyItem[] getItems() {
            return items.toArray(new StaplerOnlyItem[0]);
        }
    }

    @Test
    void describe_convertsArrayValueWhenCtorIsCollection() throws Exception {
        Stapler.CONVERT_UTILS.register(new StaplerOnlyItemConverter(), StaplerOnlyItem.class);
        try {
            CollectionCtorArrayGetter obj = new CollectionCtorArrayGetter(List.of(new StaplerOnlyItem("A")));

            ConfiguratorRegistry registry = ConfiguratorRegistry.get();
            CNode node = registry.lookupOrFail(CollectionCtorArrayGetter.class)
                    .describe(obj, new ConfigurationContext(registry));

            assertNotNull(node);

            Mapping mapping = (Mapping) node;
            assertEquals(1, mapping.get("items").asSequence().size());
        } finally {
            Stapler.CONVERT_UTILS.deregister(StaplerOnlyItem.class);
        }
    }

    @SuppressWarnings("ClassCanBeRecord")
    public static class SetCtorListGetter {
        @SuppressWarnings({"unused", "FieldCanBeLocal"})
        private final Set<String> items;

        @DataBoundConstructor
        public SetCtorListGetter(Set<String> items) {
            this.items = items;
        }

        @SuppressWarnings("unused")
        public Collection<String> getItems() {
            return Arrays.asList("A", "B");
        }
    }

    @Test
    void describe_convertsCollectionToHashSetWhenCtorIsSet() throws Exception {
        SetCtorListGetter obj = new SetCtorListGetter(Set.of("ignored"));

        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        CNode node = registry.lookupOrFail(SetCtorListGetter.class).describe(obj, new ConfigurationContext(registry));

        assertNotNull(node);
        assertInstanceOf(Mapping.class, node);

        Mapping mapping = (Mapping) node;
        assertEquals(2, mapping.get("items").asSequence().size());
    }

    @SuppressWarnings({"ClassCanBeRecord", "unused", "FieldCanBeLocal"})
    public static class SetCtorListGetterMismatch {
        private final Set<String> items;

        @DataBoundConstructor
        public SetCtorListGetterMismatch(Set<String> items) {
            this.items = items;
        }

        @SuppressWarnings("unused")
        public List<String> getItems() {
            return List.of("A", "B");
        }
    }

    @Test
    void describe_hits_set_rewrap_branch() throws Exception {
        SetCtorListGetterMismatch obj = new SetCtorListGetterMismatch(Set.of("ignored"));

        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        CNode node = registry.lookupOrFail(SetCtorListGetterMismatch.class)
                .describe(obj, new ConfigurationContext(registry));

        assertNotNull(node);
        assertInstanceOf(Mapping.class, node);

        Mapping mapping = (Mapping) node;
        assertEquals(2, mapping.get("items").asSequence().size());
    }

    @Test
    void describe_iteratesArrayAndConvertsEachItem() throws Exception {
        Stapler.CONVERT_UTILS.register(new StaplerOnlyItemConverter(), StaplerOnlyItem.class);
        try {
            CollectionCtorArrayGetter obj =
                    new CollectionCtorArrayGetter(List.of(new StaplerOnlyItem("A"), new StaplerOnlyItem("B")));

            ConfiguratorRegistry registry = ConfiguratorRegistry.get();
            CNode node = registry.lookupOrFail(CollectionCtorArrayGetter.class)
                    .describe(obj, new ConfigurationContext(registry));

            assertNotNull(node);
            Mapping mapping = (Mapping) node;
            assertEquals(2, mapping.get("items").asSequence().size());
        } finally {
            Stapler.CONVERT_UTILS.deregister(StaplerOnlyItem.class);
        }
    }

    @SuppressWarnings({"ClassCanBeRecord", "unused", "FieldCanBeLocal"})
    public static class SetCtorStaplerOnlyItemHolder {
        private final Set<StaplerOnlyItem> items;

        @DataBoundConstructor
        public SetCtorStaplerOnlyItemHolder(Set<StaplerOnlyItem> items) {
            this.items = items;
        }

        public Set<StaplerOnlyItem> getItems() {
            return items;
        }
    }

    @Test
    void describe_converts_collection_to_set_via_stapler_converter() throws Exception {
        Stapler.CONVERT_UTILS.register(new StaplerOnlyItemConverter(), StaplerOnlyItem.class);
        try {
            SetCtorStaplerOnlyItemHolder obj =
                    new SetCtorStaplerOnlyItemHolder(Set.of(new StaplerOnlyItem("A"), new StaplerOnlyItem("B")));

            ConfiguratorRegistry registry = ConfiguratorRegistry.get();

            CNode node = registry.lookupOrFail(SetCtorStaplerOnlyItemHolder.class)
                    .describe(obj, new ConfigurationContext(registry));

            assertNotNull(node, "configured instance should not be null");
            assertInstanceOf(Mapping.class, node);

            Mapping mapping = (Mapping) node;
            assertEquals(2, mapping.get("items").asSequence().size(), "items set should contain 2 elements");

        } finally {
            Stapler.CONVERT_UTILS.deregister(StaplerOnlyItem.class);
        }
    }

    @SuppressWarnings({"ClassCanBeRecord", "unused", "FieldCanBeLocal"})
    public static class SetCtorFromStrings {
        private final Set<StaplerOnlyItem> items;

        @DataBoundConstructor
        public SetCtorFromStrings(Set<StaplerOnlyItem> items) {
            this.items = items;
        }

        public Collection<String> getItems() {
            return List.of("A", "B");
        }
    }

    @Test
    void configure_converts_sequence_to_set_via_stapler_converter_for_constructor() throws Exception {
        final AtomicInteger convertCount = new AtomicInteger(0);

        Converter converter = new Converter() {
            @Override
            public <T> T convert(Class<T> type, Object value) {
                convertCount.incrementAndGet();
                if (value == null) {
                    return null;
                }
                return type.cast(new StaplerOnlyItem("converted-by-stapler-" + value));
            }
        };

        Stapler.CONVERT_UTILS.register(converter, StaplerOnlyItem.class);
        try {
            Mapping config = new Mapping();
            Sequence seq = new Sequence();
            seq.add(new Scalar("A"));
            seq.add(new Scalar("B"));
            config.put("items", seq);

            ConfiguratorRegistry registry = ConfiguratorRegistry.get();

            SetCtorFromStrings configured = (SetCtorFromStrings) registry.lookupOrFail(SetCtorFromStrings.class)
                    .configure(config, new ConfigurationContext(registry));

            assertNotNull(configured);
            assertNotNull(configured.getItems());
            assertEquals(2, configured.getItems().size());
            assertTrue(convertCount.get() >= 2, "Stapler converter should have been called once per sequence element");

        } finally {
            Stapler.CONVERT_UTILS.deregister(StaplerOnlyItem.class);
        }
    }

    @Test
    void describe_hits_set_wrap_branch_when_stapler_returns_collection() throws Exception {
        SetCtorStaplerOnlyItemHolder obj =
                new SetCtorStaplerOnlyItemHolder(Set.of(new StaplerOnlyItem("A"), new StaplerOnlyItem("B")));

        ConfiguratorRegistry registry = ConfiguratorRegistry.get();

        CNode node = registry.lookupOrFail(SetCtorStaplerOnlyItemHolder.class)
                .describe(obj, new ConfigurationContext(registry));

        assertNotNull(node);
        assertInstanceOf(Mapping.class, node);
    }
}
