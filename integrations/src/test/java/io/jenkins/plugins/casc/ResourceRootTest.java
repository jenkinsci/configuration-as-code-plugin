package io.jenkins.plugins.casc;

import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import jenkins.security.ResourceDomainConfiguration;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ResourceRootTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("resourceRoot.yml")
    public void validLoopbackUrl() {
        assertThat(ResourceDomainConfiguration.get().getUrl(), is("http://127.0.0.1:8080/"));
    }
}
