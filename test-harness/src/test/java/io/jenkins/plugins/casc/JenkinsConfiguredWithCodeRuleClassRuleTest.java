package io.jenkins.plugins.casc;

import static org.junit.Assert.assertNotNull;

import hudson.model.User;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import java.util.Collections;
import org.junit.ClassRule;
import org.junit.Test;

public class JenkinsConfiguredWithCodeRuleClassRuleTest {
    @ClassRule
    @ConfiguredWithCode("admin.yml")
    public static JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    public void user_created() {
        User admin = User.get("admin", false, Collections.emptyMap());
        assertNotNull(admin);
    }
}
