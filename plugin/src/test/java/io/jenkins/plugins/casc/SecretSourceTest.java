package io.jenkins.plugins.casc;

import io.jenkins.plugins.casc.impl.secrets.YamlSecretSource;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Optional;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class SecretSourceTest {

    @Test
    public void should_detect_var() {
        assertTrue(SecretSource.requiresReveal("${foo}").isPresent());
    }

    @Test
    public void should_detect_var_with_default_value() {
        assertTrue(SecretSource.requiresReveal("${foo:-bar}").isPresent());
    }

    @Test
    public void should_not_detect_escaped_dollar() {
        assertFalse(SecretSource.requiresReveal("\\${foo}").isPresent());
    }

    @Test
    public void should_load_secrets_file() throws IOException, URISyntaxException {
        URL resource = SecretSourceTest.class.getResource("secrets.yml");
        File secretsFile = Paths.get(resource.toURI()).toFile();
        System.setProperty("casc.secrets.config", secretsFile.getAbsolutePath());
        YamlSecretSource yamlSecretSource = new YamlSecretSource();
        Assert.assertEquals("secretvalue1", yamlSecretSource.reveal("secret1").get());
    }
}
