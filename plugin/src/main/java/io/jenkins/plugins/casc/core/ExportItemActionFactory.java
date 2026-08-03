package io.jenkins.plugins.casc.core;

import static hudson.ExtensionList.lookup;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Action;
import hudson.model.TopLevelItem;
import io.jenkins.plugins.casc.ItemConfigurator;
import java.util.Collection;
import jenkins.model.TransientActionFactory;

@Extension
public class ExportItemActionFactory extends TransientActionFactory<TopLevelItem> {

    @Override
    public Class<TopLevelItem> type() {
        return TopLevelItem.class;
    }

    @Override
    @NonNull
    public Collection<? extends Action> createFor(@NonNull TopLevelItem target) {
        for (ItemConfigurator<?> configurator : lookup(ItemConfigurator.class)) {
            if (configurator.getTarget().isAssignableFrom(target.getClass())) {
                return singletonList(new ExportItemAction(target));
            }
        }
        return emptyList();
    }
}
