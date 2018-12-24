package io.jenkins.plugins.casc.support.jobdsl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.EnvVars;
import hudson.Extension;
import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.Configurator;
import io.jenkins.plugins.casc.RootElementConfigurator;
import io.jenkins.plugins.casc.impl.attributes.MultivaluedAttribute;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Sequence;
import javaposse.jobdsl.dsl.GeneratedItems;
import javaposse.jobdsl.plugin.JenkinsDslScriptLoader;
import javaposse.jobdsl.plugin.JenkinsJobManagement;
import javaposse.jobdsl.plugin.LookupStrategy;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

// TODO: Move outside the plugin?
/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension(optional = true)
@Restricted(NoExternalUse.class)
public class SeedJobConfigurator implements RootElementConfigurator<GeneratedItems[]> {

    @Override
    public String getName() {
        return "jobs";
    }

    @Override
    public Class getTarget() {
        return GeneratedItems[].class;
    }

    @Override
    public Set<Attribute<GeneratedItems[],?>> describe() {
        return Collections.singleton(new MultivaluedAttribute("", ScriptSource.class));
    }

    @Override
    public GeneratedItems[] getTargetComponent(ConfigurationContext context) {
        return new GeneratedItems[0]; // Doesn't really make sense
    }

    @Override
    public GeneratedItems[] configure(CNode config, ConfigurationContext context) throws ConfiguratorException {
        JenkinsJobManagement mng = new JenkinsJobManagement(System.out, new EnvVars(), null, null, LookupStrategy.JENKINS_ROOT);
        final Sequence sources = config.asSequence();
        final Configurator<ScriptSource> con = context.lookup(ScriptSource.class);
        List<GeneratedItems> generated = new ArrayList<>();
        for (CNode source : sources) {
            final String script;
            try {
                script = con.configure(source, context).getScript();
                System.out.println("your script is: " + script);
            } catch (IOException e) {
                throw new ConfiguratorException(this, "Failed to retrieve job-dsl script", e);
            }
            try {
                generated.add(new JenkinsDslScriptLoader(mng).runScript(script));
            } catch (Exception ex) {
                throw new ConfiguratorException(this, "Failed to execute script with hash " + script.hashCode(), ex);
            }
        }
        return generated.toArray(new GeneratedItems[generated.size()]);
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
}
