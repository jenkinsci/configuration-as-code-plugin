package io.jenkins.plugins.casc.impl.configurators;

import hudson.Extension;
import hudson.slaves.DumbSlave;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import io.jenkins.plugins.casc.model.Scalar;
import io.jenkins.plugins.casc.model.Sequence;
import java.util.ArrayList;
import java.util.List;

@Extension
public class DumbSlaveConfigurator extends DataBoundConfigurator<DumbSlave> {

    public DumbSlaveConfigurator() {
        super(DumbSlave.class);
    }

    @Override
    protected DumbSlave instance(Mapping config, ConfigurationContext context) throws ConfiguratorException {
        CNode labelsNode = config.remove("labels");

        if (labelsNode != null) {
            List<String> allLabels = new ArrayList<>();

            CNode labelStringNode = config.remove("labelString");
            if (labelStringNode instanceof Scalar) {
                String existing = ((Scalar) labelStringNode).getValue();
                if (existing != null && !existing.trim().isEmpty()) {
                    allLabels.add(existing.trim());
                }
            }

            allLabels.addAll(flattenLabels(labelsNode));

            String combinedLabels = String.join(" ", allLabels);
            config.put("labelString", new Scalar(combinedLabels));
        }

        return super.instance(config, context);
    }

    private List<String> flattenLabels(CNode node) {
        List<String> result = new ArrayList<>();

        if (node == null) {
            return result;
        }

        if (node instanceof Scalar) {
            String value = ((Scalar) node).getValue();
            if (value != null && !value.trim().isEmpty()) {
                result.add(value.trim());
            }
        } else if (node instanceof Sequence) {
            for (CNode child : (Sequence) node) {
                result.addAll(flattenLabels(child));
            }
        }

        return result;
    }
}
