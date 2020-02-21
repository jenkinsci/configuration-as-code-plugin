package io.jenkins.plugins.casc;

import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import org.junit.Rule;
import org.junit.Test;

import static io.jenkins.plugins.casc.misc.Util.convertYamlFileToJson;
import static io.jenkins.plugins.casc.misc.Util.validateSchema;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;

public class AzureKeyVaultTest {

    @Rule
    public JenkinsConfiguredWithReadmeRule j = new JenkinsConfiguredWithReadmeRule();

    @Test
    public void validJsonSchema() throws Exception {
        assertThat(
            validateSchema(convertYamlFileToJson(this, "azureKeyVault.yml")),
            empty());
    }
}
