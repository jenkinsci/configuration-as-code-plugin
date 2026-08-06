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
            return original.equals(resolved) ? scalar : new Scalar(resolved);
        }

        if (node instanceof Mapping mapping) {
            Mapping newMapping = null;

            for (Map.Entry<String, CNode> entry : mapping.entrySet()) {
                String key = entry.getKey();
                CNode originalChild = entry.getValue();
                CNode interpolatedChild = interpolate(originalChild, context);

                if (newMapping == null && originalChild != interpolatedChild) {
                    newMapping = new Mapping();
                    for (Map.Entry<String, CNode> previous : mapping.entrySet()) {
                        if (previous.getKey().equals(key)) {
                            break;
                        }
                        newMapping.put(previous.getKey(), previous.getValue());
                    }
                }

                if (newMapping != null) {
                    newMapping.put(key, interpolatedChild);
                }
            }

            return newMapping != null ? newMapping : mapping;
        }

        if (node instanceof Sequence sequence) {
            Sequence newSequence = null;
            int index = 0;

            for (CNode child : sequence) {
                CNode interpolatedChild = interpolate(child, context);

                if (newSequence == null && child != interpolatedChild) {
                    newSequence = new Sequence();
                    for (int i = 0; i < index; i++) {
                        newSequence.add(sequence.get(i));
                    }
                }

                if (newSequence != null) {
                    newSequence.add(interpolatedChild);
                }
                index++;
            }

            return newSequence != null ? newSequence : sequence;
        }

        return node;
    }
}
