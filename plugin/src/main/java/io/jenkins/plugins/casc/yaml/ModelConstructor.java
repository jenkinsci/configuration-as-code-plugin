package io.jenkins.plugins.casc.yaml;

import io.jenkins.plugins.casc.model.Mapping;
import io.jenkins.plugins.casc.model.Scalar;
import io.jenkins.plugins.casc.model.Sequence;
import io.jenkins.plugins.casc.model.Source;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.map.AbstractMapDecorator;
import org.apache.commons.lang.ObjectUtils;
import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.constructor.Construct;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.nodes.Tag;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
class ModelConstructor extends Constructor {

    public ModelConstructor() {
        super(Mapping.class);

        this.yamlConstructors.put(Tag.BOOL, ConstructScalar);
        this.yamlConstructors.put(Tag.INT, ConstructScalar);
        this.yamlConstructors.put(Tag.STR, ConstructScalar);

    }

    private final static Construct ConstructScalar = new AbstractConstruct() {
        @Override
        public Object construct(Node node) {
            final String value = ((ScalarNode) node).getValue();
            return new Scalar(value, getSource(node));
        }
    };

    private static Source getSource(Node node) {
        final Mark mark = node.getStartMark();
        return new Source(mark.getName(), mark.getLine()+ 1);
    }

    protected Map createDefaultMap(int initSize) {
        // respect order from YAML document
        return new Mapping(initSize);
    }

    /**
     * Enforce Map keys are only Scalars and can be used as {@link String} keys in {@link Mapping}
     */
    @Override
    protected void constructMapping2ndStep(MappingNode node, final Map mapping) {
        ((Mapping) mapping).setSource(getSource(node));
        super.constructMapping2ndStep(node,
                new AbstractMapDecorator(mapping) {
            @Override
            public Object put(Object key, Object value) {
                if (!(key instanceof Scalar)) throw new IllegalStateException("We only support scalar map keys");
                Object scalar = ObjectUtils.clone(value);
                if (scalar instanceof Number) scalar = new Scalar(scalar.toString());
                else if (scalar instanceof Boolean) scalar = new Scalar(scalar.toString());

                return mapping.put(key.toString(), scalar);
            }
        });
    }

    @Override
    protected List createDefaultList(int initSize) {
        // respect order from YAML document
        return new Sequence(initSize);
    }

    @Override
    protected void constructSequenceStep2(SequenceNode node, Collection collection) {
        ((Sequence) collection).setSource(getSource(node));
        super.constructSequenceStep2(node, collection);
    }
}
