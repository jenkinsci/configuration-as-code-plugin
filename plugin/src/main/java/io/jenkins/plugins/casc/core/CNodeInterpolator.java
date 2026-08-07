package io.jenkins.plugins.casc.core;

import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import io.jenkins.plugins.casc.model.Scalar;
import io.jenkins.plugins.casc.model.Sequence;
import java.util.Map;

public class CNodeInterpolator {

    public static CNode interpolate(CNode node, ConfigurationContext context) {
        if (node == null) {
            return null;
        }

        if (node instanceof Scalar scalar) {
            String original = scalar.getValue();
            String resolved = context.getSecretSourceResolver().resolve(original);

            if (original.equals(resolved)) {
                return scalar;
            }

            return new Scalar(resolved, scalar.getSource());
        }

        if (node instanceof Mapping mapping) {
            Mapping newMapping = null;

            for (Map.Entry<String, CNode> entry : mapping.entrySet()) {
                String key = entry.getKey();
                CNode originalChild = entry.getValue();
                CNode interpolatedChild = interpolate(originalChild, context);

                if (originalChild != interpolatedChild) {
                    if (newMapping == null) {
                        newMapping = mapping.clone();
                    }
                    newMapping.put(key, interpolatedChild);
                }
            }

            return newMapping != null ? newMapping : mapping;
        }

        if (node instanceof Sequence sequence) {
            Sequence newSequence = null;

            for (int i = 0; i < sequence.size(); i++) {
                CNode child = sequence.get(i);
                CNode interpolatedChild = interpolate(child, context);

                if (child != interpolatedChild) {
                    if (newSequence == null) {
                        newSequence = sequence.clone();
                    }
                    newSequence.set(i, interpolatedChild);
                }
            }

            return newSequence != null ? newSequence : sequence;
        }

        return node;
    }
}
