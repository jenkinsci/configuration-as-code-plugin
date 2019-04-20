package io.jenkins.plugins.casc;

import hudson.security.HudsonPrivateSecurityRealm;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JenkinsConfiguredWithCodeRuleClassRuleTest {
    @ClassRule
    @ConfiguredWithCode("admin.yml")
    public static JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    public void should_be_similar_1() throws Exception {
        final HudsonPrivateSecurityRealm securityRealm = (HudsonPrivateSecurityRealm) j.jenkins.getSecurityRealm();
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        final Configurator c = context.lookupOrFail(HudsonPrivateSecurityRealm.class);
        final CNode node = c.describe(securityRealm, context);
        final Mapping user = node.asMapping().get("users").asSequence().get(0).asMapping();
        assertEquals("admin", user.getScalarValue("id"));
    }

    @Test
    public void should_be_similar_2() throws Exception {
        final HudsonPrivateSecurityRealm securityRealm = (HudsonPrivateSecurityRealm) j.jenkins.getSecurityRealm();
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        final Configurator c = context.lookupOrFail(HudsonPrivateSecurityRealm.class);
        final CNode node = c.describe(securityRealm, context);
        final Mapping user = node.asMapping().get("users").asSequence().get(0).asMapping();
        assertEquals("admin", user.getScalarValue("id"));
    }
}
