package io.jenkins.plugins.casc.core;

import static io.jenkins.plugins.casc.core.CNodeInterpolator.interpolate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.SecretSource;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import io.jenkins.plugins.casc.model.Scalar;
import io.jenkins.plugins.casc.model.Sequence;
import io.jenkins.plugins.casc.model.Source;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;

public class CNodeInterpolatorTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    private ConfigurationContext context;

    @Before
    public void setUp() {
        context = new ConfigurationContext(ConfiguratorRegistry.get());
    }

    @Test
    public void shouldReturnNullForNullNode() {
        assertNull(interpolate(null, context));
    }

    @Test
    public void shouldInterpolateScalarAndAllocateNew() {
        Scalar original = new Scalar("${VAR}");
        CNode result = interpolate(original, context);

        assertNotSame(original, result);
        assertEquals("resolved_value", result.asScalar().getValue());
    }

    @Test
    public void shouldRespectEscapedVariables() {
        Scalar original = new Scalar("^${VAR}");
        CNode result = interpolate(original, context);

        assertNotSame(original, result);
        assertEquals("${VAR}", result.asScalar().getValue());
    }

    @Test
    public void shouldResolveUnknownVariableToEmptyString() {
        Scalar original = new Scalar("${UNKNOWN}");
        CNode result = interpolate(original, context);

        assertEquals("", result.asScalar().getValue());
    }

    @Test
    public void shouldInterpolateMappingValuesButNotKeys() {
        Mapping original = new Mapping();
        original.put("${VAR}", new Scalar("${VAR}"));
        original.put("plain_key", new Scalar("plain_value"));

        CNode resultNode = interpolate(original, context);

        assertNotSame(original, resultNode);
        Mapping result = resultNode.asMapping();

        assertTrue(result.containsKey("${VAR}"));
        assertEquals("resolved_value", result.getScalarValue("${VAR}"));

        assertTrue(result.containsKey("plain_key"));
        assertEquals("plain_value", result.getScalarValue("plain_key"));
    }

    @Test
    public void shouldInterpolateSequenceAndAllocateNew() {
        Sequence original = new Sequence();
        original.add(new Scalar("plain_text"));
        original.add(new Scalar("${VAR}"));

        CNode resultNode = interpolate(original, context);

        assertNotSame(original, resultNode);
        Sequence result = resultNode.asSequence();

        assertEquals(2, result.size());
        assertEquals("plain_text", result.get(0).asScalar().getValue());
        assertEquals("resolved_value", result.get(1).asScalar().getValue());
    }

    @Test
    public void shouldReturnSameNodeWhenNothingChanges() {
        Mapping mapping = new Mapping();
        mapping.put("name", new Scalar("plain"));

        CNode result = interpolate(mapping, context);

        assertSame(mapping, result);
    }

    @Test
    public void shouldInterpolateNestedStructures() {
        Scalar targetScalar = new Scalar("${VAR}");
        Mapping innerMapping = new Mapping();
        innerMapping.put("target", targetScalar);

        Sequence sequence = new Sequence();
        sequence.add(innerMapping);
        sequence.add(new Scalar("plain_text"));

        Mapping rootMapping = new Mapping();
        rootMapping.put("sequence", sequence);
        rootMapping.put("static_sibling", new Scalar("static_value"));

        CNode resultNode = interpolate(rootMapping, context);

        assertNotSame(rootMapping, resultNode);
        Mapping result = resultNode.asMapping();

        Sequence resultSequence = result.get("sequence").asSequence();
        assertNotSame(sequence, resultSequence);

        Mapping resultInnerMapping = resultSequence.get(0).asMapping();
        assertNotSame(innerMapping, resultInnerMapping);

        assertEquals("resolved_value", resultInnerMapping.getScalarValue("target"));
        assertEquals("static_value", result.getScalarValue("static_sibling"));
    }

    @Test
    public void shouldBackfillMappingWhenSubsequentEntryChanges() {
        Mapping original = new Mapping();
        original.put("plain_key", new Scalar("plain_value"));
        original.put("var_key", new Scalar("${VAR}"));

        CNode resultNode = interpolate(original, context);

        assertNotSame(original, resultNode);
        Mapping result = resultNode.asMapping();

        assertEquals("plain_value", result.getScalarValue("plain_key"));
        assertEquals("resolved_value", result.getScalarValue("var_key"));
    }

    @Test
    public void shouldReturnSameSequenceWhenNothingChanges() {
        Sequence original = new Sequence();
        original.add(new Scalar("plain_text1"));
        original.add(new Scalar("plain_text2"));

        CNode result = interpolate(original, context);

        assertSame(original, result);
    }

    @Test
    public void shouldReturnSameNodeForUnknownCNodeType() {
        CNode customNode = new CNode() {
            @Override
            public Type getType() {
                return null;
            }

            @Override
            public Source getSource() {
                return null;
            }

            @Override
            public CNode clone() {
                try {
                    return (CNode) super.clone();
                } catch (CloneNotSupportedException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        CNode resultNode = interpolate(customNode, context);

        assertSame(customNode, resultNode);
    }

    @TestExtension
    public static class DummySecretSource extends SecretSource {
        @NonNull
        @Override
        public Optional<String> reveal(@NonNull String secret) {
            if ("VAR".equals(secret)) {
                return Optional.of("resolved_value");
            }
            return Optional.empty();
        }
    }
}
