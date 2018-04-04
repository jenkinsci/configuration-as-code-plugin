package org.jenkinsci.plugins.casc;

import hudson.EnvVars;
import hudson.Extension;
import javaposse.jobdsl.plugin.JenkinsDslScriptLoader;
import javaposse.jobdsl.plugin.JenkinsJobManagement;
import javaposse.jobdsl.plugin.LookupStrategy;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension
public class SeedJobConfigurator implements RootElementConfigurator {

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
    public Object configure(Object config) throws ConfiguratorException {
        JenkinsJobManagement mng = new JenkinsJobManagement(System.out, new EnvVars(), null, null, LookupStrategy.JENKINS_ROOT);
        for (String script : (List<String>) config) {
            try {
                new JenkinsDslScriptLoader(mng).runScript(script);
            } catch (Exception ex) {
                throw new ConfiguratorException(this, "Failed to execute script with hash " + script.hashCode(), ex);
            }
        }

        return null;
    }
}
