package io.jenkins.plugins.casc.misc;

import io.jenkins.plugins.casc.ConfigurationAsCode;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.jvnet.hudson.test.JenkinsRule;

public class JenkinsConfiguredRule extends JenkinsRule {

    //TODO: Looks like API defect, exception should be thrown
    /**
     * Exports the Jenkins configuration to a string.
     * @return YAML as string
     * @param strict Fail if any export operation returns error
     * @throws Exception Export error
     * @throws AssertionError Failed to export the configuration
     * @since 1.25
     */
    public String exportToString(boolean strict) throws Exception {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        ConfigurationAsCode.get().export(out);
        final String s = out.toString(StandardCharsets.UTF_8.name());
        if (strict && s.contains("Failed to export")) {
            throw new AssertionError("Failed to export the configuration: " + s);
        }
        return s;
    }
}
