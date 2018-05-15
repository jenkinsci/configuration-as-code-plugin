package org.jenkinsci.plugins.casc;

import hudson.EnvVars;
import hudson.Extension;
import javaposse.jobdsl.dsl.GeneratedItems;
import javaposse.jobdsl.plugin.JenkinsDslScriptLoader;
import javaposse.jobdsl.plugin.JenkinsJobManagement;
import javaposse.jobdsl.plugin.LookupStrategy;
import org.jenkinsci.plugins.casc.model.CNode;
import org.jenkinsci.plugins.casc.model.Sequence;

import javax.annotation.CheckForNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension
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
        final Sequence scripts = config.asSequence();
        List<GeneratedItems> generated = new ArrayList<>();
        for (CNode script : scripts) {
            try {
                generated.add(new JenkinsDslScriptLoader(mng).runScript(script.asScalar().getValue()));
            } catch (Exception ex) {
                throw new ConfiguratorException(this, "Failed to execute script with hash " + script.hashCode(), ex);
            }
        }
        return generated;
    }

    @CheckForNull
    @Override
    public CNode describe(List<GeneratedItems> instance) {
        return null; // FIXME
    }
}
