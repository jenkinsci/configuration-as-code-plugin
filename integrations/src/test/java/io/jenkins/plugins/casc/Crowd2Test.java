package io.jenkins.plugins.casc;

import de.theit.jenkins.crowd.CrowdSecurityRealm;
import hudson.security.SecurityRealm;
import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.RuleChain;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.jvnet.hudson.test.JenkinsMatchers.hasPlainText;

public class Crowd2Test {

    public static final String PASSWORD_123 = "password123";

    @Rule
    public RuleChain chain = RuleChain.outerRule(new EnvironmentVariables()
        .set("CROWD_PASSWORD", PASSWORD_123))
        .around(new JenkinsConfiguredWithReadmeRule());

    @Test
    @ConfiguredWithReadme("crowd2/README.md")
    public void configure_artifact_manager() throws Exception {
        SecurityRealm realm = Jenkins.get().getSecurityRealm();
        assertThat(realm, instanceOf(CrowdSecurityRealm.class));
        CrowdSecurityRealm securityRealm = (CrowdSecurityRealm) realm;
        assertThat(securityRealm.applicationName, is("jenkins"));
        assertThat(securityRealm.group, is("jenkins-users"));
        assertThat(securityRealm.url, is("http://crowd.company.io"));
        assertThat(securityRealm.password, hasPlainText(PASSWORD_123));
    }
}
