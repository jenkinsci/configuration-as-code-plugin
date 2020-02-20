package io.jenkins.plugins.casc;

import hudson.model.User;
import hudson.security.ACL;
import hudson.security.HudsonPrivateSecurityRealm;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import java.util.Collections;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JenkinsConfiguredWithCodeRuleClassRuleTest {
    @ClassRule
    @ConfiguredWithCode("admin.yml")
    public static JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    public void user_created() throws Exception {
        User admin = User.get("admin", false, Collections.emptyMap());
        assertNotNull(admin);
    }
}
