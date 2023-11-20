package io.jenkins.plugins.casc;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;

import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import jenkins.model.GlobalConfiguration;
import org.jenkinsci.plugins.gitclient.GitHostKeyVerificationConfiguration;
import org.jenkinsci.plugins.gitclient.verifier.ManuallyProvidedKeyVerificationStrategy;
import org.jenkinsci.plugins.gitclient.verifier.SshHostKeyVerificationStrategy;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * @author Mark Waite
 */
public class GitHostKeyVerificationTest {

    @ClassRule
    @ConfiguredWithReadme("git-client/README.md#1")
    public static JenkinsConfiguredWithReadmeRule j = new JenkinsConfiguredWithReadmeRule();

    @Test
    public void git_client_host_key_verifier() {
        final GitHostKeyVerificationConfiguration config =
                GlobalConfiguration.all().get(GitHostKeyVerificationConfiguration.class);
        SshHostKeyVerificationStrategy strategy = config.getSshHostKeyVerificationStrategy();
        assertThat(strategy, instanceOf(ManuallyProvidedKeyVerificationStrategy.class));
        ManuallyProvidedKeyVerificationStrategy manually = (ManuallyProvidedKeyVerificationStrategy) strategy;
        assertThat(manually.getApprovedHostKeys(), startsWith("bitbucket.org ssh-ed25519 "));
    }
}
