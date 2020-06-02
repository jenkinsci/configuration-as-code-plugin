package io.jenkins.plugins.casc.core;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Label;
import hudson.model.labels.LabelAtom;
import hudson.model.labels.LabelAtomProperty;
import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.BaseConfigurator;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.impl.attributes.MultivaluedAttribute;
import io.jenkins.plugins.casc.model.Mapping;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

@Extension
@Restricted(NoExternalUse.class)
public class LabelAtomConfigurator extends BaseConfigurator<LabelAtom> {

    @Override
    public Class<LabelAtom> getTarget() {
        return LabelAtom.class;
    }

    @Override
    protected LabelAtom instance(Mapping mapping, ConfigurationContext context) throws ConfiguratorException {
        return new LabelAtom(mapping.getScalarValue("name"));
    }

    @NonNull
    @Override
    public Set<Attribute<LabelAtom, ?>> describe() {
        return new HashSet<>(Arrays.asList(
            new Attribute<LabelAtom, String>("name", String.class)
                .getter(Label::getName),
            new MultivaluedAttribute<LabelAtom, LabelAtomProperty>("properties", LabelAtomProperty.class)
            .getter(labelAtom -> labelAtom.getProperties())
            .setter((labelAtom, properties) -> labelAtom.getProperties().addAll(properties))
        ));
    }
}
