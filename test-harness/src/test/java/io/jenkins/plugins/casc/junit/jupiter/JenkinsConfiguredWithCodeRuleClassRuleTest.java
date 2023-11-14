package io.jenkins.plugins.casc.junit.jupiter;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import hudson.model.User;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import java.util.Collections;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@WithJenkinsConfiguredWithCode
public class JenkinsConfiguredWithCodeRuleClassRuleTest {
    @ConfiguredWithCode("admin.yml")
    public static JenkinsConfiguredWithCodeRule j;

    @BeforeAll
    public static void beforeAll() {
        assertNotNull(j);
    }

    @Test
    public void user_created() {
        User admin = User.get("admin", false, Collections.emptyMap());
        assertNotNull(admin);
    }
}
