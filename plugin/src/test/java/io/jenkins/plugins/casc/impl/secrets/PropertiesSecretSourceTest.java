package io.jenkins.plugins.casc.impl.secrets;

import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.RuleChain;

public class PropertiesSecretSourceTest {

    @Rule
    public RuleChain chain = RuleChain.outerRule( new EnvironmentVariables()
        .set("SECRETS", getClass().getResource("secrets.properties").getFile()))
        .around(new JenkinsConfiguredWithCodeRule());

    @Test
    @ConfiguredWithCode("configuration-as-code.yaml")
    public void test_reading_secrets_from_properties() throws Exception {
        SystemCredentialsProvider scp = SystemCredentialsProvider.getInstance();
        UsernamePasswordCredentials credentials = (UsernamePasswordCredentials) scp.getCredentials().get(0);
        Assert.assertEquals("smasher", credentials.getUsername());
        Assert.assertEquals("-.,-.,-.,", credentials.getPassword().getPlainText());
    }
}
