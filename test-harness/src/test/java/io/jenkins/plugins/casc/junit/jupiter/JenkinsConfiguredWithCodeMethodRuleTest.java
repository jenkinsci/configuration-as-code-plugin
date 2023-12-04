package io.jenkins.plugins.casc.junit.jupiter;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import hudson.model.User;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import java.util.Collections;
import org.junit.jupiter.api.Test;

public class JenkinsConfiguredWithCodeMethodRuleTest {

    @Test
    @WithJenkinsConfiguredWithCode
    @ConfiguredWithCode("admin.yml")
    public void user_created(JenkinsConfiguredWithCodeRule rule) {
        assertNotNull(rule);
        User admin = User.get("admin", false, Collections.emptyMap());
        assertNotNull(admin);
    }
}
