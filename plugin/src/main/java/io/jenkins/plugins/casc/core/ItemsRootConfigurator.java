package io.jenkins.plugins.casc.core;

import static io.jenkins.plugins.casc.core.ItemRemoveStrategy.NONE;
import static io.jenkins.plugins.casc.core.ItemRemoveStrategy.fromString;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.Job;
import hudson.model.TopLevelItem;
import hudson.security.ACL;
import hudson.security.ACLContext;
import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.ItemConfigurator;
import io.jenkins.plugins.casc.RootElementConfigurator;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import io.jenkins.plugins.casc.model.Sequence;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;
import jenkins.model.Jenkins;

@Extension
public class ItemsRootConfigurator implements RootElementConfigurator<Jenkins> {

    private static final Logger LOGGER = Logger.getLogger(ItemsRootConfigurator.class.getName());

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

        ItemRemoveStrategy strategy = NONE;
        CNode itemsSequence = interpolatedConfig;

        if (interpolatedConfig instanceof Mapping) {
            Mapping mapping = interpolatedConfig.asMapping();
            strategy = parseRemoveStrategy(mapping);
            itemsSequence = mapping.containsKey("items") ? mapping.get("items") : new Sequence();
        }

        Set<String> configuredItemNames = new HashSet<>();

        try (ACLContext ignored = ACL.as2(ACL.SYSTEM2)) {
            for (CNode itemNode : itemsSequence.asSequence()) {
                Mapping itemMapping = itemNode.asMapping();
                Entry<String, CNode> entry = itemMapping.entrySet().iterator().next();
                String type = entry.getKey();
                Mapping properties = entry.getValue().asMapping();

                String name = properties.getScalarValue("name");
                configuredItemNames.add(name);

                ItemConfigurator<?> configurator = findConfigurator(type);
                if (configurator == null) {
                    throw new ConfiguratorException("No ItemConfigurator found for type: " + type);
                }

                TopLevelItem configuredItem = configurator.configure(name, properties, context);

                if (configuredItem instanceof Job<?, ?> job) {
                    if (job.getProperty(CascItemProperty.class) == null) {
                        try {
                            job.addProperty(new CascItemProperty());
                            job.save();
                        } catch (IOException e) {
                            LOGGER.log(WARNING, "Failed to add CasC tag property to job: " + name, e);
                        }
                    }
                }
            }

            applyRemovalStrategy(jenkins, configuredItemNames, strategy);
        }

        return jenkins;
    }

    private void applyRemovalStrategy(Jenkins jenkins, Set<String> configuredItemNames, ItemRemoveStrategy strategy)
            throws ConfiguratorException {
        if (strategy == NONE) {
            return;
        }

        try {
            for (TopLevelItem item : jenkins.getItems()) {
                if (!configuredItemNames.contains(item.getName())) {
                    boolean isCascManaged =
                            (item instanceof Job) && ((Job<?, ?>) item).getProperty(CascItemProperty.class) != null;

                    if (strategy == ItemRemoveStrategy.REMOVE_ALL) {
                        LOGGER.log(INFO, "CasC remove-all strategy: Deleting unconfigured item {0}", item.getName());
                        jenkins.remove(item);
                    } else if (strategy == ItemRemoveStrategy.SYNC && isCascManaged) {
                        LOGGER.log(
                                INFO,
                                "CasC sync strategy: Deleting previously managed, now unconfigured item {0}",
                                item.getName());
                        jenkins.remove(item);
                    }
                }
            }
        } catch (IOException | IllegalArgumentException e) {
            throw new ConfiguratorException("Failed to apply item removal strategy", e);
        }
    }

    @Override
    public Jenkins check(CNode config, ConfigurationContext context) throws ConfiguratorException {
        CNode interpolatedConfig = CNodeInterpolator.interpolate(config, context);
        return doCheck(interpolatedConfig);
    }

    private Jenkins doCheck(CNode interpolatedConfig) throws ConfiguratorException {
        CNode itemsSequence = interpolatedConfig;

        if (interpolatedConfig instanceof Mapping) {
            Mapping mapping = interpolatedConfig.asMapping();

            if (!mapping.containsKey("items") && !mapping.containsKey("removeStrategy")) {
                throw new ConfiguratorException(
                        "Invalid items configuration. Expected a sequence of items, or a mapping containing 'items' or 'removeStrategy'.");
            }
            parseRemoveStrategy(mapping);
            itemsSequence = mapping.containsKey("items") ? mapping.get("items") : new Sequence();
        }

        if (!(itemsSequence instanceof Sequence)) {
            throw new ConfiguratorException("Expected a sequence of items, found: "
                    + itemsSequence.getClass().getSimpleName());
        }

        for (CNode itemNode : itemsSequence.asSequence()) {
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

    private ItemRemoveStrategy parseRemoveStrategy(Mapping mapping) throws ConfiguratorException {
        if (!mapping.containsKey("removeStrategy")) {
            return NONE;
        }

        try {
            return fromString(mapping.get("removeStrategy").asScalar().getValue());
        } catch (IllegalArgumentException e) {
            throw new ConfiguratorException(e.getMessage());
        }
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
    public CNode describe(Jenkins instance, ConfigurationContext context) {
        return null;
    }

    @Override
    @NonNull
    public Set<Attribute<Jenkins, ?>> describe() {
        return Collections.emptySet();
    }
}
