package io.jenkins.plugins.casc;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.RuleChain;

import static org.junit.Assert.assertEquals;

public class PropertiesSecretSourceTest {

    private static final String USERNAME_SECRET = "ken";

    @Rule
    public RuleChain chain = RuleChain.outerRule(new EnvironmentVariables()
        .set("SECRETS_FILE", getClass().getResource("secrets.properties").getFile()))
        .around(new JenkinsConfiguredWithCodeRule());

    @Test
    @ConfiguredWithCode("PropertiesSecretSourceTest.yaml")
    public void testReadingSecretsFromProperties() throws Exception {
        List<UsernamePasswordCredentials> credentialList = CredentialsProvider
            .lookupCredentials(UsernamePasswordCredentials.class,
                Jenkins.getInstanceOrNull(), null, Collections.emptyList());
        assertEquals(1, credentialList.size());

        UsernamePasswordCredentials credentials = credentialList.get(0);

        // https://leahneukirchen.org/blog/archive/2019/10/ken-thompson-s-unix-password.html
        assertEquals(USERNAME_SECRET, credentials.getUsername());
        assertEquals("p/q2-q4!", credentials.getPassword().getPlainText());
    }

    @Test
    @ConfiguredWithCode("PropertiesSecretSourceTest.yaml")
    public void testSecretsFromPropertiesAreUpdatedAfterReload() throws Exception {
        File secretsFile =  new File(getClass().getResource("secrets.properties").getFile());
        Properties secrets = new Properties();
        InputStream inputStream = new FileInputStream(secretsFile);
        secrets.load(inputStream);

        FileWriter fileWriter = new FileWriter(secretsFile);

        String secretName = "testuser";
        String updatedTestUserSecret = "charmander";
        secrets.setProperty(secretName, updatedTestUserSecret);
        try {
            secrets.store(fileWriter, "store to properties file");

            ConfigurationAsCode.get().configure(this.getClass().getResource("PropertiesSecretSourceTest.yaml").toString());

            List<UsernamePasswordCredentials> credentialList = CredentialsProvider
                .lookupCredentials(UsernamePasswordCredentials.class,
                    Jenkins.getInstanceOrNull(), null, Collections.emptyList());
            assertEquals(1, credentialList.size());

            UsernamePasswordCredentials credentials = credentialList.get(0);

            assertEquals(updatedTestUserSecret, credentials.getUsername());
        } finally {
            secrets.setProperty(secretName, USERNAME_SECRET);
            secrets.store(fileWriter, "store to properties file");
        }
    }
}
