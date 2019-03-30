package io.jenkins.plugins.casc.impl.configurators;

import static io.jenkins.plugins.casc.model.CNode.Type.MAPPING;
import static io.vavr.API.unchecked;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.security.HudsonPrivateSecurityRealm;
import hudson.security.SecurityRealm;
import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.Configurator;
import io.jenkins.plugins.casc.ObsoleteConfigurationMonitor;
import io.jenkins.plugins.casc.impl.attributes.DescribableAttribute;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import io.jenkins.plugins.casc.model.Scalar;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Logger;

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
        return preConfigure(config).apply((shortName, subConfig) ->
            lookupDescriptor(shortName, config)
                .map(descriptor -> forceLookupConfigurator(context, descriptor))
                .map(configurator -> doConfigure(context, configurator, subConfig.getOrNull())))
            .getOrNull();
    }

    @Override
    public T check(CNode config, ConfigurationContext context) {
        return configure(config, context);
    }

    @NonNull
    @Override
    public Set<Attribute<T, ?>> describe() {
        return Collections.emptySet();
    }

    @CheckForNull
    @Override
    public CNode describe(T instance, ConfigurationContext context) {
        Predicate<CNode> isScalar = node -> node.getType().equals(MAPPING) && unchecked(node::asMapping).apply().size() == 0;
        return lookupConfigurator(context, instance.getClass())
                .map(configurator -> convertToNode(context, configurator, instance))
                .map(node -> {
                    if (isScalar.test(node)) {
                        return new Scalar(preferredSymbol(instance.getDescriptor()));
                    } else {
                        return new Mapping().put(preferredSymbol(instance.getDescriptor()), node);
                    }
                }).getOrNull();
    }

    @SuppressWarnings("unused")
    public Map<String, Class<T>> getImplementors() {
        return getDescriptors()
                .map(descriptor -> Tuple.of(preferredSymbol(descriptor), descriptor))
                .foldLeft(HashMap.empty(), this::handleDuplicateSymbols)
                .mapValues(this::descriptorClass)
                .toJavaMap();
    }

    @SuppressWarnings("unchecked")
    private Option<Configurator<T>> lookupConfigurator(ConfigurationContext context, Class<?> descriptor) {
        return Option.of(context.lookup(descriptor));
    }

    private Configurator<T> forceLookupConfigurator(ConfigurationContext context, Descriptor<T> descriptor) {
        Class<T> klazz = descriptorClass(descriptor);
        return lookupConfigurator(context, klazz)
                .getOrElseThrow(() -> new IllegalStateException("No configurator implementation to manage " + klazz));
    }

    private Stream<Descriptor<T>> getDescriptors() {
        return Stream.ofAll(Jenkins.getInstance().getDescriptorList(target));
    }

    @SuppressWarnings("unchecked")
    private Class<T> descriptorClass(Descriptor<T> descriptor) {
        return descriptor.getKlass().toJavaClass();
    }

    private Option<Descriptor<T>> lookupDescriptor(String symbol, CNode config) {
        return getDescriptors()
                .filter(descriptor -> findByPreferredSymbol(descriptor, symbol) || findBySymbols(descriptor, symbol, config))
                .map(descriptor -> Tuple.of(preferredSymbol(descriptor), descriptor))
                .foldLeft(HashMap.empty(), this::handleDuplicateSymbols)
                .values()
                .headOption()
                .orElse(() -> {
                    String message = "No " + target.getName() + " implementation found for " + symbol;
                    String possible = lookupPlugin("configuration-as-code-support") ? "" : System.lineSeparator() + "Possible solution: Try to install 'configuration-as-code-support' plugin";
                    throw new IllegalArgumentException(Stream.of(message, possible)
                            .intersperse("")
                            .foldLeft(new StringBuilder(), StringBuilder::append)
                            .toString());
                });
    }

    private Boolean findByPreferredSymbol(Descriptor<T> descriptor, String symbol) {
        return preferredSymbol(descriptor).equalsIgnoreCase(symbol);
    }

    private Boolean findBySymbols(Descriptor<T> descriptor, String symbol, CNode node) {
        return getSymbols(descriptor)
                .find(actual -> actual.equalsIgnoreCase(symbol))
                .map(actual -> {
                    ObsoleteConfigurationMonitor.get().record(node, "'" + symbol + "' is obsolete, please use '" + preferredSymbol(descriptor) + "'");
                    return descriptorClass(descriptor);
                }).isDefined();
    }

    private Stream<String> getSymbols(Descriptor<T> descriptor) {
        return Stream.ofAll(DescribableAttribute.getSymbols(descriptor, target, target));
    }

    private String preferredSymbol(Descriptor<?> descriptor) {
        return DescribableAttribute.getPreferredSymbol(descriptor, target, target);
    }

    private Boolean lookupPlugin(String name) {
        return Option.of(Jenkins.getInstance().getPlugin(name)).isDefined();
    }

    private HashMap<String, Descriptor<T>> handleDuplicateSymbols(HashMap<String, Descriptor<T>> r, Tuple2<String, Descriptor<T>> t) {
        if (r.containsKey(t._1)) {
            String message = String.format("Found multiple implementations for symbol = %s: [%s, %s]. Please report to plugin maintainer.", t._1, r.get(t._1).get(), t._2);
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
