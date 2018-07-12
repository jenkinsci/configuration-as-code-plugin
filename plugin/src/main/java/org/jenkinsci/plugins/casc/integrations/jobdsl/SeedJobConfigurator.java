package org.jenkinsci.plugins.casc.integrations.jobdsl;

import hudson.EnvVars;
import hudson.Extension;
import javaposse.jobdsl.dsl.GeneratedItems;
import javaposse.jobdsl.plugin.JenkinsDslScriptLoader;
import javaposse.jobdsl.plugin.JenkinsJobManagement;
import javaposse.jobdsl.plugin.LookupStrategy;
import org.jenkinsci.plugins.casc.Attribute;
import org.jenkinsci.plugins.casc.ConfiguratorException;
import org.jenkinsci.plugins.casc.RootElementConfigurator;
import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.casc.model.CNode;
import org.jenkinsci.plugins.casc.model.Sequence;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import javax.annotation.CheckForNull;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// TODO: Move outside the plugin?
/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension(optional = true)
@Restricted(NoExternalUse.class)
public class SeedJobConfigurator implements RootElementConfigurator<List<GeneratedItems>> {

    private static final Pattern FILE_SRC = Pattern.compile("\\$\\{file:([^}]*)\\}");

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
                generated.add(new JenkinsDslScriptLoader(mng).runScript(loadFromFile(script.asScalar().getValue())));
            } catch (Exception ex) {
                throw new ConfiguratorException(this, "Failed to execute script with hash " + script.hashCode(), ex);
            }
        }
        return generated;
    }

    private String loadFromFile(String value) throws IOException {
        Matcher m = FILE_SRC.matcher(value);
        if(m.matches()) {
            String fileName = m.group(1);
            return FileUtils.readFileToString(new File(fileName));
        } else {
            return value;
        }
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
