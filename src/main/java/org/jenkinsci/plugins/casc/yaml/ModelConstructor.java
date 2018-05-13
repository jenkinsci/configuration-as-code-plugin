package org.jenkinsci.plugins.casc.yaml;

import org.apache.commons.collections.map.AbstractMapDecorator;
import org.jenkinsci.plugins.casc.model.Mapping;
import org.jenkinsci.plugins.casc.model.Scalar;
import org.jenkinsci.plugins.casc.model.Sequence;
import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.constructor.Construct;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class ModelConstructor extends Constructor {

    public ModelConstructor() {
        super(Mapping.class);

        this.yamlConstructors.put(Tag.BOOL, ConstructScalar);
        this.yamlConstructors.put(Tag.INT, ConstructScalar);

    }

    private final static Construct ConstructScalar = new AbstractConstruct() {
        @Override
        public Object construct(Node node) {
            return new Scalar(((ScalarNode) node).getValue());
        }
    };

    @Override
    protected Object constructScalar(ScalarNode node) {
        return new Scalar(node.getValue());
    }

    @Override
    protected Map createDefaultMap() {
        // respect order from YAML document
        return new Mapping();
    }

    @Override
    /**
     * Enforce Map keys are only Scalars and can be used as {@link String} keys in {@link Mapping}
     */
    protected void constructMapping2ndStep(MappingNode node, final Map<Object, Object> mapping) {
        super.constructMapping2ndStep(node,
                new AbstractMapDecorator(mapping) {
            @Override
            public Object put(Object key, Object value) {
                if (!(key instanceof Scalar)) throw new IllegalStateException("We only support scalar map keys");

                if (value instanceof Number) value = new Scalar(value.toString());
                else if (value instanceof Boolean) value = new Scalar(value.toString());

                return mapping.put(key.toString(), value);
            }
        });
    }

    @Override
    protected List createDefaultList(int initSize) {
        // respect order from YAML document
        return new Sequence(initSize);
    }

}
