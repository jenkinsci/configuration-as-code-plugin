package io.jenkins.plugins.casc.core;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.security.ACL;
import hudson.security.ACLContext;
import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.ItemConfigurator;
import io.jenkins.plugins.casc.RootElementConfigurator;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.Set;
import jenkins.model.Jenkins;

@Extension
public class ItemsRootConfigurator implements RootElementConfigurator<Jenkins> {

    @Override
    @NonNull
    public String getName() {
        return "items";
    }

    @Override
    public Class<Jenkins> getTarget() {
        return Jenkins.class;
    }

    @Override
    public Jenkins getTargetComponent(ConfigurationContext context) {
        return Jenkins.get();
    }

    @Override
    @NonNull
    public Jenkins configure(CNode config, ConfigurationContext context) throws ConfiguratorException {
        CNode interpolatedConfig = CNodeInterpolator.interpolate(config, context);
        doCheck(interpolatedConfig);
        Jenkins jenkins = Jenkins.get();

        try (ACLContext ignored = ACL.as2(ACL.SYSTEM2)) {
            for (CNode itemNode : interpolatedConfig.asSequence()) {
                Mapping itemMapping = itemNode.asMapping();
                Entry<String, CNode> entry = itemMapping.entrySet().iterator().next();
                String type = entry.getKey();
                Mapping properties = entry.getValue().asMapping();

                String name = properties.getScalarValue("name");

                ItemConfigurator<?> configurator = findConfigurator(type);
                if (configurator == null) {
                    throw new ConfiguratorException("No ItemConfigurator found for type: " + type);
                }

                configurator.configure(name, properties, context);
            }
        }

        return jenkins;
    }

    private ItemConfigurator<?> findConfigurator(String type) {
        for (ItemConfigurator<?> configurator : ExtensionList.lookup(ItemConfigurator.class)) {
            if (configurator.getName().equalsIgnoreCase(type)) {
                return configurator;
            }
        }
        return null;
    }

    @Override
    public Jenkins check(CNode config, ConfigurationContext context) throws ConfiguratorException {
        CNode interpolatedConfig = CNodeInterpolator.interpolate(config, context);
        return doCheck(interpolatedConfig);
    }

    private Jenkins doCheck(CNode interpolatedConfig) throws ConfiguratorException {
        for (CNode itemNode : interpolatedConfig.asSequence()) {
            Mapping itemMapping = itemNode.asMapping();

            if (itemMapping.size() != 1) {
                throw new ConfiguratorException("Each item must have exactly one type key.");
            }

            Entry<String, CNode> entry = itemMapping.entrySet().iterator().next();
            String type = entry.getKey();
            Mapping properties = entry.getValue().asMapping();

            CNode nameNode = properties.get("name");
            if (nameNode == null) {
                throw new ConfiguratorException("Item of type '" + type + "' is missing a 'name' attribute.");
            }

            String name = nameNode.asScalar().getValue();

            if (name == null || name.trim().isEmpty()) {
                throw new ConfiguratorException("Item of type '" + type + "' must have a non-empty 'name' attribute.");
            }

            ItemConfigurator<?> configurator = findConfigurator(type);
            if (configurator == null) {
                throw new ConfiguratorException("No ItemConfigurator found for type: " + type);
            }
        }

        return Jenkins.get();
    }

    @Override
    public CNode describe(Jenkins instance, ConfigurationContext context) {
        return null;
    }

    @Override
    @NonNull
    public Set<Attribute<Jenkins, ?>> describe() {
        return Collections.emptySet();
    }
}
