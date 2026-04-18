package io.jenkins.plugins.casc.misc;

import io.jenkins.plugins.casc.CasCGlobalConfig;
import io.jenkins.plugins.casc.ConfigurationAsCode;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import jenkins.model.GlobalConfiguration;
import org.jvnet.hudson.test.JenkinsRule;

public class JenkinsConfiguredRule extends JenkinsRule {

    /**
     * Exports the Jenkins configuration to a string.
     * @return YAML as string
     * @param strict Fail if any export operation returns error
     * @throws Exception Export error
     * @since 1.25
     */
    public String exportToString(boolean strict) throws Exception {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        CasCGlobalConfig config = GlobalConfiguration.all().get(CasCGlobalConfig.class);
        boolean originalStrict = config != null && config.isStrictExport();

        if (config != null) {
            config.setStrictExport(strict);
        }

        try {
            ConfigurationAsCode.get().export(out);
        } finally {
            if (config != null) {
                config.setStrictExport(originalStrict);
            }
        }

        return out.toString(StandardCharsets.UTF_8);
    }
}
