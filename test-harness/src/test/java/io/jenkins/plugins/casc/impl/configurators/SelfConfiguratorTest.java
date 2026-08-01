package io.jenkins.plugins.casc.impl.configurators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import io.jenkins.plugins.casc.ConfigurationAsCode;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@WithJenkinsConfiguredWithCode
class SelfConfiguratorTest {

    @Test
    @ConfiguredWithCode(value = "SelfConfiguratorTest.yml")
    void self_configure(JenkinsConfiguredWithCodeRule j) {
        assertThat(j.jenkins.getRawBuildsDir(), is("/tmp"));
    }

    @Test
    @ConfiguredWithCode(value = "SelfConfiguratorRestrictedTest.yml", expected = ConfiguratorException.class)
    void self_configure_restricted(JenkinsConfiguredWithCodeRule j) {
        // expected to throw Configurator Exception
        assertThat(j.jenkins.getRawBuildsDir(), is(not("/tmp")));
    }

    @Test
    @SuppressWarnings("unused")
    void export_configuration_as_code_defaults(JenkinsConfiguredWithCodeRule j) throws Exception {
        ConfigurationContext context = new ConfigurationContext(null);
        SelfConfigurator configurator = new SelfConfigurator();

        CNode node = configurator.describe(context, context);

        assertThat(node, is(notNullValue()));
        Mapping mapping = node.asMapping();

        assertThat(mapping.getScalarValue("version"), is("1"));
        assertThat(mapping.getScalarValue("deprecated"), is("reject"));
        assertThat(mapping.getScalarValue("restricted"), is("reject"));
        assertThat(mapping.getScalarValue("unknown"), is("reject"));
    }

    @Test
    @SuppressWarnings("unused")
    void export_pipeline_contains_configuration_as_code(JenkinsConfiguredWithCodeRule j) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        ConfigurationAsCode.get().export(out);
        String exportedYaml = out.toString(StandardCharsets.UTF_8);

        assertThat(exportedYaml, containsString("configuration-as-code:"));
        assertThat(exportedYaml, containsString("version: \"1\""));
        assertThat(exportedYaml, containsString("deprecated: \"reject\""));
        assertThat(exportedYaml, containsString("restricted: \"reject\""));
        assertThat(exportedYaml, containsString("unknown: \"reject\""));
    }
}
