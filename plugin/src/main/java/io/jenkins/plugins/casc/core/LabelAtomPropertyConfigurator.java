package io.jenkins.plugins.casc.core;

import hudson.Extension;
import hudson.model.labels.LabelAtomProperty;
import io.jenkins.plugins.casc.impl.configurators.HeteroDescribableConfigurator;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

@Extension
@Restricted(NoExternalUse.class)
public class LabelAtomPropertyConfigurator extends HeteroDescribableConfigurator<LabelAtomProperty> {

    public LabelAtomPropertyConfigurator() {
        super(LabelAtomProperty.class);
    }

}


