package io.jenkins.plugins.casc.impl.configurators;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import io.jenkins.plugins.casc.BaseConfigurator;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.RootElementConfigurator;
import io.jenkins.plugins.casc.UnknownAttributesException;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import java.util.List;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension(ordinal = Double.MAX_VALUE)
@Restricted(NoExternalUse.class)
public class SelfConfigurator extends BaseConfigurator<ConfigurationContext>
        implements RootElementConfigurator<ConfigurationContext> {

    @NonNull
    @Override
    public String getName() {
        return "configuration-as-code";
    }

    @Override
    public Class<ConfigurationContext> getTarget() {
        return ConfigurationContext.class;
    }

    @Override
    public ConfigurationContext getTargetComponent(ConfigurationContext context) {
        return context;
    }

    @Override
    protected ConfigurationContext instance(Mapping mapping, ConfigurationContext context) {
        return context;
    }

    protected void configure(
            Mapping config, ConfigurationContext instance, boolean dryrun, ConfigurationContext context)
            throws ConfiguratorException {
        // ConfigurationContext has to be configured _even_ for dry-run as it determine CasC behaviour
        CNode decryption = config.remove("decryption");
        super.configure(config, instance, false, context);
        if (decryption != null) {
            Mapping dm = decryption.asMapping();
            CNode jwe = dm.remove("jwe");
            if (jwe == null) {
                throw ConfiguratorException.from(
                        decryption, "Missing required configuration: 'decryption.jwe.keyFile'.");
            }
            Mapping jm = jwe.asMapping();
            CNode keyFile = jm.remove("keyFile");
            if (keyFile == null) {
                throw ConfiguratorException.from(jwe, "Missing required configuration: 'decryption.jwe.keyFile'.");
            }
            instance.setDecryption(new ConfigurationContext.DecryptionConfiguration());
            try {
                instance.getDecryption()
                        .setJwe(new ConfigurationContext.JweConfiguration(
                                keyFile.asScalar().getValue()));
            } catch (IllegalArgumentException e) {
                throw ConfiguratorException.from(keyFile, e.getMessage());
            }
            if (!jm.isEmpty() || !dm.isEmpty()) {
                String invalid = !dm.isEmpty() ? String.join(",", dm.keySet()) : String.join(",", jm.keySet());
                String message = "Invalid decryption configuration elements: " + invalid;
                if (instance.getUnknown() == ConfigurationContext.Unknown.reject) {
                    throw new UnknownAttributesException(this, message, message, invalid, List.of("jwe"));
                }
                context.warning(decryption, message);
            }
        }
    }

    @CheckForNull
    @Override
    public CNode describe(ConfigurationContext instance, ConfigurationContext context) throws Exception {
        Mapping mapping = new Mapping();
        mapping.put("version", instance.getVersion().value());
        mapping.put("deprecated", instance.getDeprecated().name());
        mapping.put("restricted", instance.getRestricted().name());
        mapping.put("unknown", instance.getUnknown().name());
        return mapping;
    }
}
