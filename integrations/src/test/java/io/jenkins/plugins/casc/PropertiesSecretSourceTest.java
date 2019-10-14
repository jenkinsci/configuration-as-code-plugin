package io.jenkins.plugins.casc;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import java.util.Collections;
import java.util.List;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.RuleChain;

import static org.junit.Assert.assertEquals;

public class PropertiesSecretSourceTest {

    @Rule
    public RuleChain chain = RuleChain.outerRule(new EnvironmentVariables()
        .set("SECRETS", getClass().getResource("secrets.properties").getFile()))
        .around(new JenkinsConfiguredWithCodeRule());

    @Test
    @ConfiguredWithCode("PropertiesSecretSourceTest.yaml")
    public void test_reading_secrets_from_properties() throws Exception {
        List<UsernamePasswordCredentials> credentialList = CredentialsProvider
            .lookupCredentials(UsernamePasswordCredentials.class,
                Jenkins.getInstanceOrNull(), null, Collections.emptyList());
        assertEquals(1, credentialList.size());

        UsernamePasswordCredentials credentials = credentialList.get(0);

        // https://leahneukirchen.org/blog/archive/2019/10/ken-thompson-s-unix-password.html
        assertEquals("ken", credentials.getUsername());
        assertEquals("p/q2-q4!", credentials.getPassword().getPlainText());
    }
}
