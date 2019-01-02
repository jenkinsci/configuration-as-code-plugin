package io.jenkins.plugins.casc.support.jobdsl;

import static io.vavr.API.Try;
import static io.vavr.API.unchecked;

import hudson.EnvVars;
import hudson.Extension;
import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.Configurator;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.RootElementConfigurator;
import io.jenkins.plugins.casc.SecretSourceResolver;
import io.jenkins.plugins.casc.impl.attributes.MultivaluedAttribute;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import javaposse.jobdsl.dsl.GeneratedItems;
import javaposse.jobdsl.plugin.JenkinsDslScriptLoader;
import javaposse.jobdsl.plugin.JenkinsJobManagement;
import javaposse.jobdsl.plugin.LookupStrategy;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

// TODO: Move outside the plugin?
/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension(optional = true)
@Restricted(NoExternalUse.class)
public class SeedJobConfigurator implements RootElementConfigurator<GeneratedItems[]> {

    @Nonnull
    @Override
    public String getName() {
        return "jobs";
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class getTarget() {
        return GeneratedItems[].class;
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public Set<Attribute<GeneratedItems[], ?>> describe() {
        return Collections.singleton(new MultivaluedAttribute("", ScriptSource.class));
    }

    @Override
    public GeneratedItems[] getTargetComponent(ConfigurationContext context) {
        return new GeneratedItems[0]; // Doesn't really make sense
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public GeneratedItems[] configure(CNode config, ConfigurationContext context) throws ConfiguratorException {
        JenkinsJobManagement management = new JenkinsJobManagement(System.out, new EnvVars(), null, null, LookupStrategy.JENKINS_ROOT);
        Configurator<ScriptSource> configurator = context.lookupOrFail(ScriptSource.class);
        return config.asSequence().stream()
                .map(source -> getActualValue(source, context))
                .map(source -> getScriptFromSource(source, context, configurator))
                .map(script -> generateFromScript(script, management))
                .toArray(GeneratedItems[]::new);
    }

    @Override
    public GeneratedItems[] check(CNode config, ConfigurationContext context) throws ConfiguratorException {
        // Any way to dry-run a job-dsl script ?
        return new GeneratedItems[0];
    }

    @Nonnull
    @Override
    public List<Configurator> getConfigurators(ConfigurationContext context) {
        return Collections.singletonList(context.lookup(ScriptSource.class));
    }

    @CheckForNull
    @Override
    public CNode describe(GeneratedItems[] instance, ConfigurationContext context) throws Exception {
        return null;
    }

    private CNode getActualValue(CNode config, ConfigurationContext context) {
        return unchecked(() -> config.asMapping().entrySet().stream().findFirst()).apply()
                .map(entry -> {
                    Mapping mapping = new Mapping();
                    mapping.put(entry.getKey(), revealSourceOrGetValue(entry, context));
                    return (CNode) mapping;
                }).orElse(config);
    }

    private String revealSourceOrGetValue(Map.Entry<String, CNode> entry, ConfigurationContext context) {
        String value = unchecked(() -> entry.getValue().asScalar().getValue()).apply();
        return SecretSourceResolver.resolve(context, value);
    }

    private GeneratedItems generateFromScript(String script, JenkinsJobManagement management) {
        return unchecked(() ->
                Try(() -> new JenkinsDslScriptLoader(management).runScript(script))
                        .getOrElseThrow(t -> new ConfiguratorException(this, "Failed to execute script with hash " + script.hashCode(), t))).apply();
    }

    private String getScriptFromSource(CNode source, ConfigurationContext context, Configurator<ScriptSource> configurator) {
        return unchecked(() ->
                Try(() -> configurator.configure(source, context))
                        .getOrElseThrow(t -> new ConfiguratorException(this, "Failed to retrieve job-dsl script", t))
                        .getScript()).apply();
    }
}
