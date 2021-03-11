package io.jenkins.plugins.casc.impl.configurators;

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
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.PostConstruct;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.LoggerRule;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import static io.jenkins.plugins.casc.misc.Util.assertLogContains;
import static io.jenkins.plugins.casc.misc.Util.assertNotInLog;
import static io.jenkins.plugins.casc.misc.Util.getJenkinsRoot;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class DataBoundConfiguratorTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Rule
    public LoggerRule logging = new LoggerRule();

    @Before
    public void tearUp() {
        logging.record(Logger.getLogger(DataBoundConfigurator.class.getName()), Level.FINEST).capture(2048);
    }

    @Test
    public void configure_databound() throws Exception {
        Mapping config = new Mapping();
        config.put("foo", "foo");
        config.put("bar", "true");
        config.put("qix", "123");
        config.put("zot", "DataBoundSetter");
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        final Foo configured = (Foo) registry.lookupOrFail(Foo.class).configure(config, new ConfigurationContext(registry));
        assertEquals("foo", configured.foo);
        assertTrue(configured.bar);
        assertEquals(123, configured.qix);
        assertEquals("DataBoundSetter", configured.zot);
        assertThat(configured.initialized, is(true));
    }

    @Test
    public void exportYaml() throws Exception {
        Foo foo = new Foo("foo", true, 42);
        foo.setZot("zot");
        foo.setDbl(12.34);
        foo.setFlt(1f); // whole numbers are exported as "<number>.0"
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        final Configurator c = registry.lookupOrFail(Foo.class);
        final ConfigurationContext context = new ConfigurationContext(registry);
        final CNode node = c.describe(foo, context);
        assertNotNull(node);
        assertTrue(node instanceof Mapping);
        Mapping map = (Mapping) node;
        assertEquals(map.get("foo").toString(), "foo");
        assertEquals(map.get("bar").toString(), "true");
        assertEquals(map.get("qix").toString(), "42");
        assertEquals(map.get("zot").toString(), "zot");
        assertEquals(map.get("dbl").toString(), "12.34");
        assertEquals(map.get("flt").toString(), "1.0");
        assertEquals(Util.toYamlString(map.get("foo")).trim(), "\"foo\"");
        assertEquals(Util.toYamlString(map.get("bar")).trim(), "true");
        assertEquals(Util.toYamlString(map.get("qix")).trim(), "42");
        assertEquals(Util.toYamlString(map.get("zot")).trim(), "\"zot\"");
        assertEquals(Util.toYamlString(map.get("dbl")).trim(), "\"12.34\"");
        assertEquals(Util.toYamlString(map.get("flt")).trim(), "\"1.0\"");
        assertFalse(map.containsKey("other"));
    }

    @Test
    public void configureWithSets() throws Exception {
        Mapping config = new Mapping();
        Sequence sequence = new Sequence();
        sequence.add(new Scalar("bar"));
        sequence.add(new Scalar("foo"));
        config.put("strings", sequence);
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        final Bar configured = (Bar) registry.lookupOrFail(Bar.class).configure(config, new ConfigurationContext(registry));
        Set<String> strings = configured.getStrings();
        assertTrue(strings.contains("foo"));
        assertTrue(strings.contains("bar"));
        assertFalse(strings.contains("baz"));
    }

    @Test
    public void configureWithEmptySet() throws Exception {
        Mapping config = new Mapping();
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        final Bar configured = (Bar) registry.lookupOrFail(Bar.class).configure(config, new ConfigurationContext(registry));
        Set<String> strings = configured.getStrings();
        assertEquals(0, strings.size());
    }

    @Test
    public void nonnullConstructorParameter() throws Exception {
        Mapping config = new Mapping();
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        final NonnullParameterConstructor configured = (NonnullParameterConstructor) registry
                                                        .lookupOrFail(NonnullParameterConstructor.class)
                                                        .configure(config, new ConfigurationContext(registry));
        assertEquals(0, configured.getStrings().size());
    }

    @Test
    public void classParametersAreNonnullByDefault() throws Exception {
        Mapping config = new Mapping();
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        final ClassParametersAreNonnullByDefault configured = (ClassParametersAreNonnullByDefault) registry
                                                        .lookupOrFail(ClassParametersAreNonnullByDefault.class)
                                                        .configure(config, new ConfigurationContext(registry));
        assertTrue(configured.getStrings().isEmpty());
    }

    @Test
    public void packageParametersAreNonnullByDefault() {
        Mapping config = new Mapping();
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();

        String expectedMessage = "string is required to configure class io.jenkins.plugins.casc.impl.configurators.nonnull.nonnullparampackage.PackageParametersAreNonnullByDefault";

        ConfiguratorException exception = assertThrows(ConfiguratorException.class, () -> registry
            .lookupOrFail(PackageParametersAreNonnullByDefault.class)
            .configure(config, new ConfigurationContext(registry)));

       assertThat(exception.getMessage(), is(expectedMessage));
    }

    @Test
    @Issue("#1025")
    public void packageParametersAreNonnullByDefaultButCanBeNullable() throws Exception {
        Mapping config = new Mapping();
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        final PackageParametersNonNullCheckForNull configured = (PackageParametersNonNullCheckForNull) registry
            .lookupOrFail(PackageParametersNonNullCheckForNull.class)
            .configure(config, new ConfigurationContext(registry));
        assertNull(configured.getSecret());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void exportWithSets() throws Exception {
        HashSet<String> set = new HashSet<>();
        set.add("foo");

        Bar bar = new Bar(set);
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        final Configurator c = registry.lookupOrFail(Bar.class);
        final ConfigurationContext context = new ConfigurationContext(registry);
        CNode node = c.describe(bar, context);
        assertNotNull(node);
        assertTrue(node instanceof Mapping);
        Mapping map = (Mapping) node;
        assertEquals(map.get("strings").toString(), "[foo]");
        assertEquals(Util.toYamlString(map.get("strings")).trim(),"- \"foo\"");
        assertFalse(map.containsKey("other"));

        // now with two elements
        set.add("bar");
        node = c.describe(bar, context);
        assertNotNull(node);
        assertTrue(node instanceof Mapping);
        map = (Mapping) node;
        assertEquals(map.get("strings").toString(), "[bar, foo]");
        assertEquals(Util.toYamlString(map.get("strings")).trim(), "- \"bar\"\n- \"foo\"");
    }


    @Test
    @Issue("PR #838, Issue #222")
    public void export_mapping_should_not_be_null() throws Exception {
        j.createFreeStyleProject("testJob1");
        ConfigurationAsCode casc = ConfigurationAsCode.get();
        casc.configure(this.getClass().getResource("DataBoundDescriptorNonNull.yml")
                .toString());

        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        final Mapping configNode = getJenkinsRoot(context);
        final CNode viewsNode = configNode.get("views");
        Mapping listView = viewsNode.asSequence().get(1).asMapping().get("list").asMapping();
        Mapping otherListView = viewsNode.asSequence().get(2).asMapping().get("list").asMapping();
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
    public void shouldThrowConfiguratorException() {
        Mapping config = new Mapping();
        config.put("foo", "foo");
        config.put("bar", "abcd");
        config.put("qix", "99");
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        try {
            registry.lookupOrFail(Foo.class).configure(config, new ConfigurationContext(registry));
            fail("above action is excepted to throw ConfiguratorException!");
        } catch (ConfiguratorException e) {
            assertThat(e.getMessage(), is("foo: Failed to construct instance of class io.jenkins.plugins.casc.impl.configurators.DataBoundConfiguratorTest$Foo.\n" +
                    " Constructor: public io.jenkins.plugins.casc.impl.configurators.DataBoundConfiguratorTest$Foo(java.lang.String,boolean,int).\n" +
                    " Arguments: [java.lang.String, java.lang.Boolean, java.lang.Integer].\n" +
                    " Expected Parameters: foo java.lang.String, bar boolean, qix int"));
        }
    }

    @Test
    public void shouldNotLogSecrets() throws Exception {
        Mapping config = new Mapping();
        config.put("secret", "mySecretValue");
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        registry.lookupOrFail(SecretHolder.class).configure(config, new ConfigurationContext(registry));
        assertLogContains(logging, "secret");
        assertNotInLog(logging, "mySecretValue");
    }

    @Test
    @Issue("SECURITY-1497")
    public void shouldNotLogSecretsForUndefinedConstructors() throws Exception {
        Mapping config = new Mapping();
        config.put("secret", "mySecretValue");
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        registry.lookupOrFail(SecretHolderWithString.class).configure(config, new ConfigurationContext(registry));
        assertLogContains(logging, "secret");
        assertNotInLog(logging, "mySecretValue");
    }

    @Test
    public void shouldExportArray() throws Exception {
        ArrayConstructor obj = new ArrayConstructor(new Foo[]{new Foo("", false, 0)});

        ConfiguratorRegistry registry = ConfiguratorRegistry.get();

        final Configurator c = registry.lookupOrFail(ArrayConstructor.class);
        final ConfigurationContext context = new ConfigurationContext(registry);
        CNode node = c.describe(obj, context);

        assertNotNull(node);
        assertTrue(node instanceof Mapping);
        Mapping map = (Mapping) node;
        assertEquals(map.get("anArray").toString(), "[{qix=0, bar=false, foo=}]");
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

        public float getFlt() { return flt; }

        @DataBoundSetter
        public void setFlt(float flt) { this.flt = flt; }
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
}
