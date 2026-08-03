package io.jenkins.plugins.casc.core;

import hudson.ExtensionList;
import hudson.model.TopLevelItem;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.Configurator;
import io.jenkins.plugins.casc.ItemConfigurator;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import io.jenkins.plugins.casc.model.Sequence;

public class ItemExporter {

    public CNode export(TopLevelItem item, ConfigurationContext context) throws Exception {
        ItemConfigurator<?> configurator = findConfigurator(item.getClass());
        if (configurator == null) {
            return null;
        }

        @SuppressWarnings("unchecked")
        Configurator<Object> baseConfigurator = (Configurator<Object>) configurator;
        CNode itemNode = baseConfigurator.describe(item, context);

        if (itemNode == null) {
            return null;
        }

        Mapping typeMapping = new Mapping();
        typeMapping.put(configurator.getName(), itemNode);

        Sequence itemsSeq = new Sequence();
        itemsSeq.add(typeMapping);

        Mapping root = new Mapping();
        root.put("items", itemsSeq);

        return root;
    }

    private ItemConfigurator<?> findConfigurator(Class<?> clazz) {
        for (ItemConfigurator<?> configurator : ExtensionList.lookup(ItemConfigurator.class)) {
            if (configurator.getTarget().isAssignableFrom(clazz)) {
                return configurator;
            }
        }
        return null;
    }
}
