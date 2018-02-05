package org.jenkinsci.plugins.casc;

import hudson.EnvVars;
import hudson.Extension;
import javaposse.jobdsl.plugin.JenkinsDslScriptLoader;
import javaposse.jobdsl.plugin.JenkinsJobManagement;
import javaposse.jobdsl.plugin.LookupStrategy;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jenkinsci.plugins.casc.ConfigurationAsCode.CASC_JENKINS_CONFIG;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension
public class SeedJobConfigurator implements RootElementConfigurator {

    public static final Pattern JOB_DSL_FILE_PATTERN = Pattern.compile("jobDslFile:(.*)");

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
    public Object configure(Object config) throws Exception {
        JenkinsJobManagement mng = new JenkinsJobManagement(System.out, new EnvVars(), null, null, LookupStrategy.JENKINS_ROOT);
        for (String key : (List<String>) config) {
            String script = getScript(key);
            new JenkinsDslScriptLoader(mng).runScript(script);
        }

        return null;
    }

    private String getScript(String key) throws IOException {
        Matcher m = JOB_DSL_FILE_PATTERN.matcher(key);
        if(m.matches()) {
            String configPath = System.getProperty(CASC_JENKINS_CONFIG, System.getenv(CASC_JENKINS_CONFIG));
            String dslFile = m.group(1);
            if (StringUtils.isBlank(configPath) || Paths.get(dslFile).isAbsolute()) {
                return FileUtils.readFileToString(new File(dslFile));
            } else {
                return FileUtils.readFileToString(new File(configPath, dslFile));
            }
        }
        return key;
    }

}
