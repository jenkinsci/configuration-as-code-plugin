package io.jenkins.plugins.casc.impl.configurators;

import static io.jenkins.plugins.casc.model.CNode.Type.MAPPING;
import static io.vavr.API.unchecked;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.slaves.OfflineCause;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.DescriptorExtensionList;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.security.HudsonPrivateSecurityRealm;
import hudson.security.SecurityRealm;
import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.Configurator;
import io.jenkins.plugins.casc.ObsoleteConfigurationMonitor;
import io.jenkins.plugins.casc.UnknownAttributesException;
import io.jenkins.plugins.casc.impl.attributes.DescribableAttribute;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import io.jenkins.plugins.casc.model.Scalar;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * {@link Configurator} that works with {@link Describable} subtype as a {@link #target}.
 *
 * <p>
 * The configuration object will be specify the 'short name' which we use to resolve to a specific
 * subtype of {@link #target}. For example, if {@link #target} is {@link SecurityRealm} and the short name
 * is 'local', we resolve to {@link HudsonPrivateSecurityRealm} (because it has {@link Symbol} annotation that
 * specifies that name.
 *
 * <p>
 * The corresponding {@link Configurator} will be then invoked to configure the chosen subtype.
 *
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Restricted(NoExternalUse.class)
public class HeteroDescribableConfigurator<T extends Describable<T>> implements Configurator<T> {

    private static final Logger LOGGER = Logger.getLogger(HeteroDescribableConfigurator.class.getName());

    // NEW: keys for Node offline handling via JCasC
    private static final String KEY_TEMP_OFFLINE = "temporarilyOffline";
    private static final String KEY_OFFLINE_MSG = "offlineCauseMessage";

    private final Class<T> target;

    public HeteroDescribableConfigurator(Class<T> clazz) {
        this.target = clazz;
    }

    @Override
    public Class<T> getTarget() {
        return target;
    }

    @NonNull
    @Override
    public List<Configurator<T>> getConfigurators(ConfigurationContext context) {
        return getDescriptors()
                .flatMap(d -> lookupConfigurator(context, descriptorClass(d)))
                .append(this)
                .toJavaList();
    }

    @NonNull
    @Override
    public T configure(CNode config, ConfigurationContext context) {
        // Split the hetero-describable mapping into (symbol, subConfig)
        Tuple2<String, Option<CNode>> tuple = preConfigure(config);
        String shortName = tuple._1;
        Option<CNode> subConfigOpt = tuple._2;

        // Find underlying configurator for the chosen subtype
        Configurator<T> configurator = lookupDescriptor(shortName, config)
                .map(d -> forceLookupConfigurator(context, d))
                .getOrElseThrow(() -> new IllegalStateException("No configurator for " + shortName));

        CNode subConfigNode = subConfigOpt.getOrNull();

        // If this configurator is handling Nodes, peel off our custom keys before delegating,
        // then apply them to the created Node afterward.
        Boolean tempOffline = null;
        String offlineMsg = null;

        if (Node.class.isAssignableFrom(target) && subConfigNode != null && subConfigNode.getType().equals(MAPPING)) {
            Mapping m = unchecked(subConfigNode::asMapping).apply();

            // Extract and remove `temporarilyOffline`
            CNode offlineFlagNode = m.remove(KEY_TEMP_OFFLINE);
            if (offlineFlagNode != null) {
                try {
                    Scalar s = unchecked(offlineFlagNode::asScalar).apply();
                    tempOffline = Boolean.parseBoolean(s.getValue());
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Invalid value for " + KEY_TEMP_OFFLINE + ", expected boolean", e);
                }
            }

            // Extract and remove `offlineCauseMessage`
            CNode offlineMsgNode = m.remove(KEY_OFFLINE_MSG);
            if (offlineMsgNode != null) {
                try {
                    Scalar s = unchecked(offlineMsgNode::asScalar).apply();
                    offlineMsg = s.getValue();
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Invalid value for " + KEY_OFFLINE_MSG + ", expected string", e);
                }
            }
        }

        // Delegate to the specific subtype configurator
        T instance = unchecked(() -> configurator.configure(subConfigNode, context)).apply();

        // Apply offline state if we are dealing with a Node
        if (instance instanceof Node) {
            Node n = (Node) instance;
            try {
                Computer c = n.toComputer(); // may be null at this point during early boot
                if (c != null) {
                    if (Boolean.TRUE.equals(tempOffline)) {
                        String msg = (offlineMsg != null && !offlineMsg.isEmpty()) ? offlineMsg : "Configured via JCasC";
                        c.setTemporarilyOffline(true, new OfflineCause.ByCLI(msg));
                    } else if (Boolean.FALSE.equals(tempOffline)) {
                        c.setTemporarilyOffline(false, null);
                    }
                    // If only a message is given and node is already temp-offline, update the cause
                    if (offlineMsg != null && c.isTemporarilyOffline()) {
                        c.setOfflineCause(new OfflineCause.ByCLI(offlineMsg));
                    }
                } else {
                    // Computer not yet initialized; nothing else we can do without adding listeners (no new files allowed)
                    if (tempOffline != null || offlineMsg != null) {
                        LOGGER.log(Level.FINE, "Computer not available yet for node {0}; offline flags will not be applied now.", n.getNodeName());
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to apply offline state for node " + n.getNodeName(), e);
            }
        }

        return instance;
    }

    @Override
    public T check(CNode config, ConfigurationContext context) {
        return configure(config, context);
    }

    @NonNull
    @Override
    public Set<Attribute<T, ?>> describe() {
        // No statically-declared attributes here; we intercept in configure() instead.
        return Collections.emptySet();
    }

    @CheckForNull
    @Override
    public CNode describe(T instance, ConfigurationContext context) {
        Predicate<CNode> isScalar = node -> node.getType().equals(MAPPING)
                && unchecked(node::asMapping).apply().size() == 0;

        return lookupConfigurator(context, instance.getClass())
                .map(configurator -> convertToNode(context, configurator, instance))
                .filter(Objects::nonNull)
                .map(node -> {
                    // If the underlying node config is a mapping, and this is a Node instance,
                    // augment the exported YAML with temporarilyOffline/offlineCauseMessage when applicable.
                    if (instance instanceof Node && node.getType().equals(MAPPING)) {
                        Node n = (Node) instance;
                        try {
                            Computer c = n.toComputer();
                            if (c != null && c.isTemporarilyOffline()) {
                                Mapping sub = unchecked(node::asMapping).apply();
                                sub.put(KEY_TEMP_OFFLINE, new Scalar("true"));
                                if (c.getOfflineCause() != null) {
                                    sub.put(KEY_OFFLINE_MSG, new Scalar(c.getOfflineCause().toString()));
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.log(Level.FINE, "Ignoring error while exporting offline state for node " + n.getNodeName(), e);
                        }
                    }

                    if (isScalar.test(node)) {
                        return new Scalar(preferredSymbol(((Describable<?>) instance).getDescriptor()));
                    } else {
                        final Mapping mapping = new Mapping();
                        mapping.put(preferredSymbol(((Describable<?>) instance).getDescriptor()), node);
                        return mapping;
                    }
                })
                .getOrNull();
    }

    @SuppressWarnings("unused")
    public Map<String, Class<T>> getImplementors() {
        return getDescriptors()
                .map(descriptor -> Tuple.of(preferredSymbol(descriptor), descriptor))
                .foldLeft(HashMap.empty(), this::handleDuplicateSymbols)
                .mapValues(this::descriptorClass)
                .toJavaMap();
    }

    private Option<Configurator<T>> lookupConfigurator(ConfigurationContext context, Class<?> descriptor) {
        return Option.of(context.lookup(descriptor));
    }

    private Configurator<T> forceLookupConfigurator(ConfigurationContext context, Descriptor<T> descriptor) {
        Class<T> klazz = descriptorClass(descriptor);
        return lookupConfigurator(context, klazz)
                .getOrElseThrow(() -> new IllegalStateException("No configurator implementation to manage " + klazz));
    }

    /**
     * Matches suitable descriptors for the target.
     * The fetch is trivial when the target implements a root {@link Describable} object.
     * If not, we iterate to parent classes until we find a class which can provide the descriptor list in {@link Jenkins#getDescriptorList(Class)}.
     * Then we go through all the descriptors and find ones compliant with the target.
     * @return Stream of descriptors which match the target
     */
    private Stream<Descriptor<T>> getDescriptors() {
        DescriptorExtensionList<T, Descriptor<T>> descriptorList = Jenkins.get().getDescriptorList(target);
        if (!descriptorList.isEmpty()) { // fast fetch for root objects
            return Stream.ofAll(descriptorList);
        }

        LOGGER.log(
                Level.FINEST,
                "getDescriptors(): Cannot find descriptors for {0}."
                        + "Will try parent classes to find proper extension points",
                target);
        DescriptorExtensionList parentDescriptorClassList = descriptorList;
        Class<?> effectiveTarget = target.getSuperclass();
        while (parentDescriptorClassList.isEmpty() && effectiveTarget != null && effectiveTarget != Object.class) {
            final Class<Describable> match;
            LOGGER.log(Level.FINEST, "getDescriptors() for {0}: Trying parent class {1}", new Object[] {
                target, effectiveTarget
            });
            try {
                match = (Class<Describable>) effectiveTarget;
            } catch (Exception ex) {
                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.log(
                            Level.FINEST,
                            String.format(
                                    "getDescriptors() for %s: Class %s is not describable, interrupting search",
                                    target, effectiveTarget),
                            ex);
                }
                break;
            }
            parentDescriptorClassList = Jenkins.get().getDescriptorList(match);
            effectiveTarget = effectiveTarget.getSuperclass();
        }

        if (parentDescriptorClassList.isEmpty()) {
            return Stream.empty();
        }

        List<Descriptor<T>> descriptorsWithProperType = new ArrayList<>();
        Iterator<Descriptor> iterator = parentDescriptorClassList.iterator();
        while (iterator.hasNext()) {
            Descriptor<T> d = iterator.next();
            try {
                descriptorsWithProperType.add(d);
                LOGGER.log(
                        Level.FINEST,
                        "getDescriptors() for {0}: Accepting {1} as a suitable descriptor",
                        new Object[] {target, d});
            } catch (ClassCastException ex) {
                // ignored
                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.log(
                            Level.FINEST,
                            String.format(
                                    "getDescriptors() for %s: Ignoring %s, because it is not a proper describable type",
                                    target, d),
                            ex);
                }
            }
        }
        return Stream.ofAll(descriptorsWithProperType);
    }

    @SuppressWarnings("unchecked")
    private Class<T> descriptorClass(Descriptor<T> descriptor) {
        return descriptor.getKlass().toJavaClass();
    }

    private Option<Descriptor<T>> lookupDescriptor(String symbol, CNode config) {
        Stream<Descriptor<T>> descriptors = getDescriptors();
        return descriptors
                .filter(descriptor ->
                        findByPreferredSymbol(descriptor, symbol) || findBySymbols(descriptor, symbol, config))
                .map(descriptor -> Tuple.of(preferredSymbol(descriptor), descriptor))
                .foldLeft(HashMap.empty(), this::handleDuplicateSymbols)
                .values()
                .headOption()
                .orElse(() -> {
                    List<String> availableImplementations = descriptors
                            .toJavaStream()
                            .map(d -> DescribableAttribute.getPreferredSymbol(d, getImplementedAPI(), target))
                            .collect(Collectors.toList());

                    throw new UnknownAttributesException(
                            this,
                            "No implementation found for:",
                            "No " + target.getName() + " implementation found for " + symbol,
                            symbol,
                            availableImplementations);
                });
    }

    private Boolean findByPreferredSymbol(Descriptor<T> descriptor, String symbol) {
        return preferredSymbol(descriptor).equalsIgnoreCase(symbol);
    }

    private Boolean findBySymbols(Descriptor<T> descriptor, String symbol, CNode node) {
        return getSymbols(descriptor)
                .find(actual -> actual.equalsIgnoreCase(symbol))
                .map(actual -> {
                    ObsoleteConfigurationMonitor.get()
                            .record(
                                    node,
                                    "'" + symbol + "' is obsolete, please use '" + preferredSymbol(descriptor) + "'");
                    return descriptorClass(descriptor);
                })
                .isDefined();
    }

    private Stream<String> getSymbols(Descriptor<T> descriptor) {
        return Stream.ofAll(DescribableAttribute.getSymbols(descriptor, target, target));
    }

    private String preferredSymbol(Descriptor<?> descriptor) {
        return DescribableAttribute.getPreferredSymbol(descriptor, target, target);
    }

    private HashMap<String, Descriptor<T>> handleDuplicateSymbols(
            HashMap<String, Descriptor<T>> r, Tuple2<String, Descriptor<T>> t) {
        if (r.containsKey(t._1)) {
            String message = String.format(
                    "Found multiple implementations for symbol = %s: [%s, %s]. Please report to plugin maintainer.",
                    t._1, r.get(t._1).get(), t._2);
            LOGGER.warning(message);
            return r;
        } else {
            return r.put(t);
        }
    }

    private Tuple2<String, Option<CNode>> preConfigure(CNode config) {
        switch (config.getType()) {
            case SCALAR:
                return configureScalar(config);
            case MAPPING:
                return configureMapping(config);
            default:
                return configureUnexpected(config);
        }
    }

    private Tuple2<String, Option<CNode>> configureUnexpected(CNode config) {
        throw new IllegalArgumentException("Unexpected configuration type " + config);
    }

    private Tuple2<String, Option<CNode>> configureScalar(CNode config) {
        Scalar scalar = unchecked(config::asScalar).apply();
        return Tuple.of(scalar.getValue(), Option.none());
    }

    private Tuple2<String, Option<CNode>> configureMapping(CNode config) {
        Mapping mapping = unchecked(config::asMapping).apply();
        if (mapping.size() != 1) {
            throw new IllegalArgumentException("Single entry map expected to configure a " + target.getName());
        } else {
            Map.Entry<String, CNode> next = mapping.entrySet().iterator().next();
            return Tuple.of(next.getKey(), Option.some(next.getValue()));
        }
    }

    private T doConfigure(ConfigurationContext context, Configurator<T> configurator, CNode subConfig) {
        return unchecked(() -> configurator.configure(subConfig, context)).apply();
    }

    @SuppressWarnings("unchecked")
    private CNode convertToNode(ConfigurationContext context, Configurator configurator, Describable instance) {
        return unchecked(() -> configurator.describe(instance, context)).apply();
    }
}
