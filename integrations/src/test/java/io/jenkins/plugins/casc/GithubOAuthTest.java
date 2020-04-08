package io.jenkins.plugins.casc;

import hudson.security.SecurityRealm;
import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.GithubSecurityRealm;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.RuleChain;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;


/**
 * Purpose:
 *  Test that we can configure: <a href="https://plugins.jenkins.io/github-oauth"/>
 */
public class GithubOAuthTest {

    @Rule
    public RuleChain chain = RuleChain.outerRule(new EnvironmentVariables()
        .set("GITHUB_SECRET", "j985j8fhfhh377"))
        .around(new JenkinsConfiguredWithReadmeRule());

    @Test
    @ConfiguredWithReadme("github-oauth/README.md")
    public void testSampleVersionForOAuth() {
        SecurityRealm realm = Jenkins.get().getSecurityRealm();
        assertThat(realm, instanceOf(GithubSecurityRealm.class));
        GithubSecurityRealm gsh = (GithubSecurityRealm)realm;
        assertEquals("someId", gsh.getClientID());
        assertEquals("https://api.github.com", gsh.getGithubApiUri());
        assertEquals("https://github.com", gsh.getGithubWebUri());
        assertEquals("j985j8fhfhh377", gsh.getClientSecret().getPlainText());
        assertEquals("read:org,user:email", gsh.getOauthScopes());
    }
}
