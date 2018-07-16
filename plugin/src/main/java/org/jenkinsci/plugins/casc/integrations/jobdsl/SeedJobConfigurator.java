package org.jenkinsci.plugins.casc.integrations.jobdsl;

import hudson.EnvVars;
import hudson.Extension;
import javaposse.jobdsl.dsl.GeneratedItems;
import javaposse.jobdsl.plugin.JenkinsDslScriptLoader;
import javaposse.jobdsl.plugin.JenkinsJobManagement;
import javaposse.jobdsl.plugin.LookupStrategy;
import org.jenkinsci.plugins.casc.Attribute;
import org.jenkinsci.plugins.casc.Configurator;
import org.jenkinsci.plugins.casc.ConfiguratorException;
import org.jenkinsci.plugins.casc.RootElementConfigurator;
import org.jenkinsci.plugins.casc.model.CNode;
import org.jenkinsci.plugins.casc.model.Sequence;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import javax.annotation.CheckForNull;
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
public class SeedJobConfigurator implements RootElementConfigurator<List<GeneratedItems>> {

    @Override
    public String getName() {
        return "jobs";
    }

    @Override
    public Set<Attribute> describe() {
        // no attribute this is a raw list
        return Collections.EMPTY_SET;
    }

    @Override
    public List<GeneratedItems> getTargetComponent() {
        return Collections.EMPTY_LIST; // Doesn't really make sense
    }

    @Override
    public List<GeneratedItems> configure(CNode config) throws ConfiguratorException {
        JenkinsJobManagement mng = new JenkinsJobManagement(System.out, new EnvVars(), null, null, LookupStrategy.JENKINS_ROOT);
        final Sequence sources = config.asSequence();
        final Configurator<ScriptSource> con = Configurator.lookup(ScriptSource.class);
        List<GeneratedItems> generated = new ArrayList<>();
        for (CNode source : sources) {
            final String script;
            try {
                script = con.configure(source).getScript();
            } catch (IOException e) {
                throw new ConfiguratorException(this, "Failed to retrieve job-dsl script", e);
            }
            try {
                generated.add(new JenkinsDslScriptLoader(mng).runScript(script));
            } catch (Exception ex) {
                throw new ConfiguratorException(this, "Failed to execute script with hash " + script.hashCode(), ex);
            }
        }
        return generated;
    }

    @Override
    public List<GeneratedItems> check(CNode config) throws ConfiguratorException {
        // Any way to dry-run a job-dsl script ?
        return Collections.emptyList();
    }

    @CheckForNull
    @Override
    public CNode describe(List<GeneratedItems> instance) {
        return null; // FIXME
    }


}
