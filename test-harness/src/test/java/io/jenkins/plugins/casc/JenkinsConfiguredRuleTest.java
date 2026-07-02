package io.jenkins.plugins.casc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import io.jenkins.plugins.casc.misc.JenkinsConfiguredRule;
import java.util.Objects;
import jenkins.model.GlobalConfiguration;
import org.junit.Rule;
import org.junit.Test;

public class JenkinsConfiguredRuleTest {

    @Rule
    public JenkinsConfiguredRule j = new JenkinsConfiguredRule();

    @Test
    public void exportToString_restoresOriginalState() throws Exception {
        CasCGlobalConfig config = GlobalConfiguration.all().get(CasCGlobalConfig.class);
        Objects.requireNonNull(config).setStrictExport(false);

        j.exportToString(true);

        assertThat(config.isStrictExport(), is(false));
    }
}
