package org.jenkinsci.plugins.casc;

import hudson.model.ManagementLink;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.casc.misc.CodeConfiguratorRunner;
import org.jenkinsci.plugins.casc.misc.ConfiguredWithCode;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.jvnet.hudson.test.JenkinsRule;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class GetConfiguratorsTest {

    public JenkinsRule j = new JenkinsRule();
    public CodeConfiguratorRunner config = new CodeConfiguratorRunner();

    @Rule
    public RuleChain chain = RuleChain.outerRule(j)
            .around(config);

    @Test
    @ConfiguredWithCode(value = "GetConfiguratorsTest.yml")
    public void shouldGetAllConfigurators() throws Exception {
        final Jenkins jenkins = Jenkins.getInstance();
        ConfigurationAsCode configurationAsCode = jenkins.getExtensionList(ManagementLink.class)
                .get(ConfigurationAsCode.class);
        assertThat(configurationAsCode.getConfigurators(), hasSize(greaterThan(0)));
    }
}
