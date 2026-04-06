package io.jenkins.plugins.casc.yaml;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Objects;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

public class YamlExtendsTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    private ConfigurationContext context;

    @Before
    public void setUp() {
        context = new ConfigurationContext(null) {
            @Override
            public String getMergeStrategy() {
                return "";
            }

            @Override
            public int getYamlCodePointLimit() {
                return 3 * 1024 * 1024;
            }

            @Override
            public int getYamlMaxAliasesForCollections() {
                return 50;
            }
        };
    }

    @Test
    public void testSingleExtendsDeepMerge() throws Exception {
        writeYaml("base.yaml", """
                jenkins:
                  systemMessage: 'Base Message'
                  numExecutors: 2
                """);

        Path child = writeYaml("child.yaml", """
                _extends: base.yaml
                jenkins:
                  systemMessage: 'Override Message'
                """);

        Node root = parse(child);
        assertTrue("Root should be a MappingNode", root instanceof MappingNode);

        MappingNode map = (MappingNode) root;
        MappingNode jenkinsMap = (MappingNode) getChildNode(map, "jenkins");

        assertEquals("Override Message", getScalarValue(jenkinsMap, "systemMessage"));
        assertEquals("2", getScalarValue(jenkinsMap, "numExecutors"));
    }

    @Test
    public void testMultipleExtendsSequentialOverride() throws Exception {
        writeYaml("base1.yaml", "a: 1\nb: 1");
        writeYaml("base2.yaml", "b: 2\nc: 2");

        Path child = writeYaml("child.yaml", """
                _extends: [base1.yaml, base2.yaml]
                c: 3
                d: 4""");

        Node root = parse(child);
        assertTrue(root instanceof MappingNode);
        MappingNode map = (MappingNode) root;

        assertEquals("1", getScalarValue(map, "a"));
        assertEquals("2", getScalarValue(map, "b"));
        assertEquals("3", getScalarValue(map, "c"));
        assertEquals("4", getScalarValue(map, "d"));
    }

    @Test
    public void testCircularDependencyThrowsException() throws Exception {
        writeYaml("a.yaml", "_extends: b.yaml\nfoo: bar");
        Path b = writeYaml("b.yaml", "_extends: a.yaml\nbar: baz");

        ConfiguratorException thrown = expectConfiguratorException(() -> parse(b));

        assertThat(
                "Exception should mention circular dependencies",
                thrown.getMessage(),
                containsString("Circular _extends dependency detected"));
    }

    @Test
    public void testTypeMismatchMapVsSequenceThrowsException() throws Exception {
        writeYaml("list.yaml", """
            - item1
            - item2
            """);

        Path child = writeYaml("child.yaml", """
            _extends: list.yaml
            key: value
            """);

        ConfiguratorException thrown = expectConfiguratorException(() -> parse(child));

        assertThat(
                "Exception should catch the type mismatch",
                thrown.getMessage(),
                containsString("Type mismatch during merge"));
    }

    @Test
    public void testRelativePathResolutionSibling() throws Exception {
        Path subDir = Files.createDirectory(tempDir.getRoot().toPath().resolve("subdir"));
        writeYaml(subDir.resolve("base.yaml"), "val: success");

        Path child = writeYaml(subDir.resolve("child.yaml"), "_extends: base.yaml");

        Node root = parse(child);
        assertEquals("success", getScalarValue((MappingNode) root, "val"));
    }

    @Test
    public void testEmptyExtendsThrowsException() throws Exception {
        Path emptyStringExtends = writeYaml("empty_str.yaml", "_extends: ''");
        ConfiguratorException ex1 = assertThrows(ConfiguratorException.class, () -> parse(emptyStringExtends));
        assertThat(ex1.getMessage(), containsString("The '_extends' property cannot be empty."));

        Path emptyListExtends = writeYaml("empty_list.yaml", "_extends: []");
        ConfiguratorException ex2 = assertThrows(ConfiguratorException.class, () -> parse(emptyListExtends));
        assertThat(ex2.getMessage(), containsString("The '_extends' list cannot be empty."));

        Path nullExtends = writeYaml("null_item.yaml", "_extends: [ base.yaml, '' ]");
        ConfiguratorException ex3 = assertThrows(ConfiguratorException.class, () -> parse(nullExtends));
        assertThat(ex3.getMessage(), containsString("Items in the '_extends' list cannot be null"));
    }

    private Path writeYaml(String filename, String content) throws IOException {
        return writeYaml(tempDir.getRoot().toPath().resolve(filename), content);
    }

    private Path writeYaml(Path path, String content) throws IOException {
        Files.writeString(path, content);
        return path;
    }

    private Node parse(Path file) throws ConfiguratorException {
        YamlSource<Path> source = YamlSource.of(file);
        Node merged = YamlUtils.merge(Collections.singletonList(source), context);

        return Objects.requireNonNull(merged, "Parsed YAML returned null (is the file empty?)");
    }

    private Node getChildNode(MappingNode parent, String key) {
        for (NodeTuple tuple : parent.getValue()) {
            ScalarNode keyNode = (ScalarNode) tuple.getKeyNode();
            if (key.equals(keyNode.getValue())) {
                return tuple.getValueNode();
            }
        }
        throw new AssertionError("Expected key '" + key + "' was not found in the YAML mapping.");
    }

    private String getScalarValue(MappingNode parent, String key) {
        Node child = getChildNode(parent, key);

        if (child instanceof ScalarNode) {
            return ((ScalarNode) child).getValue();
        }

        throw new AssertionError("Expected key '" + key + "' to be a scalar string, but found a " + child.getNodeId());
    }

    @Test
    public void testExtendsUsesSameFileMultipleTimesWithoutCircularError() throws Exception {
        writeYaml("base.yaml", "a: 1");

        Path child = writeYaml("child.yaml", """
            _extends: base.yaml
            b: 2
            c:
              _extends: base.yaml
            """);

        Node root = parse(child);
        MappingNode map = (MappingNode) root;

        assertEquals("1", getScalarValue(map, "a"));
        assertEquals("2", getScalarValue(map, "b"));

        MappingNode cNode = (MappingNode) getChildNode(map, "c");
        assertEquals("1", getScalarValue(cNode, "a"));
    }

    @Test
    public void testRelativeExtendsWithoutBaseContextThrowsException() {
        String uiYamlContent = """
                _extends: base.yaml
                jenkins:
                  systemMessage: 'Hello'
                """;

        InputStream inputStream = new ByteArrayInputStream(uiYamlContent.getBytes(StandardCharsets.UTF_8));

        YamlSource<InputStream> source = YamlSource.of(inputStream);

        ConfiguratorException thrown = assertThrows(
                ConfiguratorException.class, () -> YamlUtils.merge(Collections.singletonList(source), context));

        assertThat(
                "Exception should catch relative paths being used without a base context",
                thrown.getMessage(),
                containsString("Relative `_extends` paths"));
    }

    @Test
    public void testDeepNestedMultiLevelExtends() throws Exception {
        writeYaml("grandparent.yaml", "grandparentKey: 'grandparentValue'");

        writeYaml("parent.yaml", """
            _extends: grandparent.yaml
            parentKey: 'parentValue'
            """);

        Path childFile = writeYaml("child.yaml", """
            _extends: parent.yaml
            childKey: 'childValue'
            """);

        Node root = parse(childFile);
        assertTrue(root instanceof MappingNode);
        MappingNode map = (MappingNode) root;

        assertEquals("grandparentValue", getScalarValue(map, "grandparentKey"));
        assertEquals("parentValue", getScalarValue(map, "parentKey"));
        assertEquals("childValue", getScalarValue(map, "childKey"));
    }

    private ConfiguratorException expectConfiguratorException(Runnable r) {
        Exception ex = assertThrows(Exception.class, r::run);

        if (ex instanceof ConfiguratorException) {
            return (ConfiguratorException) ex;
        }

        if (ex.getCause() instanceof ConfiguratorException) {
            return (ConfiguratorException) ex.getCause();
        }

        throw new AssertionError("Expected ConfiguratorException but got: " + ex);
    }

    @Test
    public void testExtendsListWithNonScalarItemThrowsException() throws Exception {
        writeYaml("base.yaml", "a: 1");

        Path child = writeYaml("child.yaml", """
            _extends:
              - base.yaml
              - key: value
            """);

        ConfiguratorException ex = expectConfiguratorException(() -> parse(child));

        assertThat(ex.getMessage(), containsString("Invalid item in '_extends'"));
    }

    @Test
    public void testExtendsWithInvalidTypeThrowsException() throws Exception {
        Path child = writeYaml("child.yaml", """
            _extends:
              key: value
            """);

        ConfiguratorException ex = expectConfiguratorException(() -> parse(child));

        assertThat(ex.getMessage(), containsString("Invalid value for '_extends' key"));
    }

    @Test
    public void testExtendsWithUnreadableFileThrowsException() throws Exception {
        Path dir = tempDir.newFolder("not_a_file").toPath();

        Path child = writeYaml("child.yaml", "_extends: " + dir.getFileName().toString());

        ConfiguratorException ex = expectConfiguratorException(() -> parse(child));

        assertThat(ex.getMessage(), containsString("Failed to read extended config"));
    }

    @Test
    public void testMappingWithoutExtendsButWithNestedChangesReturnsNewMapNode() throws Exception {
        Path yaml = writeYaml("simple.yaml", """
            jenkins:
              systemMessage: "hello"
            """);

        Node root = parse(yaml);

        assertTrue(root instanceof MappingNode);
        MappingNode map = (MappingNode) root;

        MappingNode jenkins = (MappingNode) getChildNode(map, "jenkins");
        assertEquals("hello", getScalarValue(jenkins, "systemMessage"));
    }

    @Test
    public void testSequenceNodeWithExtendsTriggersHasChanges() throws Exception {
        writeYaml("base.yaml", """
            key: baseValue
            """);

        Path child = writeYaml("child.yaml", """
            list:
              - _extends: base.yaml
            """);

        Node root = parse(child);
        assertTrue(root instanceof MappingNode);

        SequenceNode list = (SequenceNode) getChildNode((MappingNode) root, "list");

        MappingNode item = (MappingNode) list.getValue().get(0);
        assertEquals("baseValue", getScalarValue(item, "key"));
    }

    @Test
    public void testExtendsWithFileUri() throws Exception {
        Path base = writeYaml("base.yaml", """
            key: uriValue
            """);

        String uriPath = base.toUri().toString();

        Path child = writeYaml("child.yaml", "_extends: " + uriPath);

        Node root = parse(child);

        assertTrue(root instanceof MappingNode);
        assertEquals("uriValue", getScalarValue((MappingNode) root, "key"));
    }

    @Test
    public void testExtendsWithHttpUriFormat() {
        String fakeUri = "https://example.com/config.yaml";

        ConfiguratorException ex =
                assertThrows(ConfiguratorException.class, () -> parse(writeYaml("child.yaml", "_extends: " + fakeUri)));

        assertThat(ex.getMessage(), containsString("Failed to read extended config"));
    }

    @Test
    public void testExtendsWithNonExistentFileThrowsException() throws Exception {
        Path child = writeYaml("child.yaml", """
            _extends: missing.yaml
            key: value
            """);

        ConfiguratorException ex = assertThrows(ConfiguratorException.class, () -> parse(child));

        assertThat(ex.getMessage(), containsString("Extended configuration file does not exist"));
    }

    @Test
    public void testExtendsWithStringSourceUri() throws Exception {
        writeYaml("base.yaml", "key: baseValue");

        Path childFile = writeYaml("child.yaml", """
            _extends: base.yaml
            """);

        String childUri = childFile.toUri().toString();

        YamlSource<String> source = YamlSource.of(childUri);

        Node root = YamlUtils.merge(Collections.singletonList(source), context);

        assertEquals("baseValue", getScalarValue((MappingNode) root, "key"));
    }

    @Test
    public void testUnsupportedSourceTypeThrowsException() throws Exception {
        Object unsupported = new Object();
        YamlSource<Object> source = new YamlSource<>(unsupported);

        Method resolveMethod =
                YamlUtils.class.getDeclaredMethod("resolveRelativeSource", YamlSource.class, String.class);
        resolveMethod.setAccessible(true);

        InvocationTargetException ex = assertThrows(
                InvocationTargetException.class, () -> resolveMethod.invoke(null, source, "dummy-path.yaml"));

        Throwable actualException = ex.getCause();
        assertTrue(actualException instanceof ConfiguratorException);

        assertThat(
                actualException.getMessage(),
                containsString("Cannot resolve relative path 'dummy-path.yaml' for source type: Object"));
    }

    @Test
    public void testInvalidYamlKeyTypeThrowsException() throws Exception {
        writeYaml("base.yaml", """
            validKey: value
            """);

        Path child = writeYaml("child.yaml", """
            _extends: base.yaml
            ? [a, b]
            : value
            """);

        ConfiguratorException ex = assertThrows(ConfiguratorException.class, () -> parse(child));

        assertThat(ex.getMessage(), containsString("Invalid YAML key type"));
    }

    @Test
    public void testSequenceOverrideUsesCloneNode() throws Exception {
        writeYaml("base.yaml", """
            list:
              - one
              - two
            """);

        Path child = writeYaml("child.yaml", """
            _extends: base.yaml
            list:
              - three
              - four
            """);

        Node root = parse(child);

        SequenceNode seq = (SequenceNode) getChildNode((MappingNode) root, "list");

        assertEquals(2, seq.getValue().size());
        assertEquals("three", ((ScalarNode) seq.getValue().get(0)).getValue());
    }

    @Test
    public void testCloneNodeWithNullValue() throws Exception {
        writeYaml("base.yaml", """
            key: value
            """);

        Path child = writeYaml("child.yaml", """
            _extends: base.yaml
            key:
            """);

        Node root = parse(child);

        MappingNode map = (MappingNode) root;

        Node valueNode = getChildNode(map, "key");

        assertTrue(valueNode == null || valueNode instanceof ScalarNode);
    }

    @Test
    public void testCloneNodeWithAliasReturnsOriginalNode() throws Exception {
        Path file = writeYaml("alias.yaml", """
            base: &anchor
              key: value
            copy: *anchor
            """);

        Node root = parse(file);

        MappingNode map = (MappingNode) root;

        Node copyNode = getChildNode(map, "copy");

        assertNotNull(copyNode);
    }

    @Test
    public void testCloneNodeFallbackBranch() throws Exception {
        Path file = writeYaml("complex.yaml", """
            root:
              - &a { key: value }
              - *a
            """);

        Node root = parse(file);

        assertNotNull(root);
    }

    @Test
    public void testInvalidUriFallbackBranch() {
        String bad = "ht!tp://invalid uri";

        assertThrows(Exception.class, () -> {
            YamlSource<String> source = YamlSource.of(bad);
            YamlUtils.merge(Collections.singletonList(source), context);
        });
    }
}
