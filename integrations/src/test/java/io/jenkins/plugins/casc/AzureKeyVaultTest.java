package io.jenkins.plugins.casc;

import static io.jenkins.plugins.casc.misc.Util.convertYamlFileToJson;
import static io.jenkins.plugins.casc.misc.Util.validateSchema;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;

import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import org.junit.Rule;
import org.junit.Test;

public class AzureKeyVaultTest {

    @Rule
    public JenkinsConfiguredWithReadmeRule j = new JenkinsConfiguredWithReadmeRule();

    @Test
    public void validJsonSchema() throws Exception {
        assertThat(validateSchema(convertYamlFileToJson(this, "azureKeyVault.yml")), empty());
    }
}
