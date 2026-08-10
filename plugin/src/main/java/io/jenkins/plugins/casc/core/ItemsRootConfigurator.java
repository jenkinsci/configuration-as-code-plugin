package io.jenkins.plugins.casc.core;

import static io.jenkins.plugins.casc.Attribute.noop;
import static io.jenkins.plugins.casc.core.ItemRemoveStrategy.KEEP;
import static io.jenkins.plugins.casc.core.ItemRemoveStrategy.fromString;
import static java.lang.Thread.currentThread;
import static java.util.Collections.unmodifiableSet;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.TopLevelItem;
import hudson.security.ACL;
import hudson.security.ACLContext;
import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.BaseConfigurator;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.ItemConfigurator;
import io.jenkins.plugins.casc.RootElementConfigurator;
import io.jenkins.plugins.casc.impl.attributes.MultivaluedAttribute;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import io.jenkins.plugins.casc.model.Sequence;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;
import jenkins.model.Jenkins;

@Extension
public class ItemsRootConfigurator extends BaseConfigurator<ItemsRootConfigurator>
        implements RootElementConfigurator<ItemsRootConfigurator> {

    private static final Logger LOGGER = Logger.getLogger(ItemsRootConfigurator.class.getName());

    @Override
    @NonNull
    public String getName() {
        return "items";
    }

    @Override
    public Class<ItemsRootConfigurator> getTarget() {
        return ItemsRootConfigurator.class;
    }

    @Override
    public ItemsRootConfigurator getTargetComponent(ConfigurationContext context) {
        return this;
    }

    @Override
    @NonNull
    public ItemsRootConfigurator configure(CNode config, ConfigurationContext context) throws ConfiguratorException {
        CNode interpolatedConfig = CNodeInterpolator.interpolate(config, context);
        doCheck(interpolatedConfig);
        Jenkins jenkins = Jenkins.get();

        ItemRemoveStrategy actionOnUndeclaredItems = KEEP;
        CNode itemsSequence = interpolatedConfig;

        if (interpolatedConfig instanceof Mapping) {
            Mapping mapping = interpolatedConfig.asMapping();
            actionOnUndeclaredItems = parseActionOnUndeclaredItems(mapping);
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

                File cascMarker = new File(configuredItem.getRootDir(), ".casc-managed");
                File parentDir = cascMarker.getParentFile();

                if (!parentDir.exists()) {
                    throw new ConfiguratorException("Cannot create CasC marker for item '" + name
                            + "': The item's root directory does not exist. "
                            + "This indicates the item was not properly saved to disk.");
                }

                try {
                    if (!cascMarker.exists() && !cascMarker.createNewFile()) {
                        LOGGER.log(
                                WARNING, "Failed to create CasC marker file (it may already exist) for item: " + name);
                    }
                } catch (IOException e) {
                    throw new ConfiguratorException("Failed to write CasC marker file for item: " + name, e);
                }
            }

            applyRemovalStrategy(jenkins, configuredItemNames, actionOnUndeclaredItems);
        }
        return this;
    }

    private void applyRemovalStrategy(Jenkins jenkins, Set<String> configuredItemNames, ItemRemoveStrategy strategy)
            throws ConfiguratorException {
        if (strategy == KEEP) {
            return;
        }

        try {
            for (TopLevelItem item : jenkins.getItems()) {
                if (!configuredItemNames.contains(item.getName())) {

                    boolean isCascManaged = new File(item.getRootDir(), ".casc-managed").exists();

                    if (strategy == ItemRemoveStrategy.DELETE_ALL) {
                        LOGGER.log(INFO, "CasC remove-all strategy: Deleting unconfigured item {0}", item.getName());
                        item.delete();
                    } else if (strategy == ItemRemoveStrategy.DELETE_TRACKED && isCascManaged) {
                        LOGGER.log(
                                INFO,
                                "CasC sync strategy: Deleting previously managed, now unconfigured item {0}",
                                item.getName());
                        item.delete();
                    }
                }
            }
        } catch (IOException | IllegalArgumentException e) {
            throw new ConfiguratorException("Failed to apply item removal strategy", e);
        } catch (InterruptedException e) {
            currentThread().interrupt();
            throw new ConfiguratorException("Interrupted while applying item removal strategy", e);
        }
    }

    @Override
    public ItemsRootConfigurator check(CNode config, ConfigurationContext context) throws ConfiguratorException {
        CNode interpolatedConfig = CNodeInterpolator.interpolate(config, context);
        doCheck(interpolatedConfig);
        return this;
    }

    private void doCheck(CNode interpolatedConfig) throws ConfiguratorException {
        CNode itemsSequence = interpolatedConfig;

        if (interpolatedConfig instanceof Mapping) {
            Mapping mapping = interpolatedConfig.asMapping();

            if (!mapping.containsKey("items") && !mapping.containsKey("actionOnUndeclaredItems")) {
                throw new ConfiguratorException(
                        "Invalid items configuration. Expected a sequence of items, or a mapping containing 'items' or 'actionOnUndeclaredItems'.");
            }

            for (String key : mapping.keySet()) {
                if (!key.equals("items") && !key.equals("actionOnUndeclaredItems")) {
                    throw new ConfiguratorException("Invalid items configuration. Unsupported key '" + key
                            + "'. Only 'items' and 'actionOnUndeclaredItems' are allowed.");
                }
            }

            parseActionOnUndeclaredItems(mapping);
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
            if (nameNode.getType() != CNode.Type.SCALAR) {
                throw new ConfiguratorException("Item of type '" + type + "' must have a string 'name' attribute.");
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

        Jenkins.get();
    }

    private ItemRemoveStrategy parseActionOnUndeclaredItems(Mapping mapping) throws ConfiguratorException {
        if (!mapping.containsKey("actionOnUndeclaredItems")) {
            return KEEP;
        }

        try {
            return fromString(mapping.get("actionOnUndeclaredItems").asScalar().getValue());
        } catch (IllegalArgumentException e) {
            throw new ConfiguratorException(e.getMessage());
        }
    }

    private ItemConfigurator<?> findConfigurator(String type) {
        return ExtensionList.lookup(ItemConfigurator.class).stream()
                .filter(c -> c.getName().equalsIgnoreCase(type))
                .findFirst()
                .orElse(null);
    }

    @Override
    protected ItemsRootConfigurator instance(Mapping mapping, ConfigurationContext context)
            throws ConfiguratorException {
        return this;
    }

    @Override
    public CNode describe(ItemsRootConfigurator instance, ConfigurationContext context) {
        return null;
    }

    @Override
    @NonNull
    public Set<Attribute<ItemsRootConfigurator, ?>> describe() {
        Set<Attribute<ItemsRootConfigurator, ?>> attributes = new HashSet<>();

        attributes.add(new MultivaluedAttribute<ItemsRootConfigurator, TopLevelItem>("items", TopLevelItem.class)
                .getter(target -> Jenkins.get().getItems())
                .setter(noop()));

        attributes.add(new Attribute<ItemsRootConfigurator, ItemRemoveStrategy>(
                        "actionOnUndeclaredItems", ItemRemoveStrategy.class)
                .setter(noop()));

        return unmodifiableSet(attributes);
    }
}
