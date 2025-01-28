package io.jenkins.plugins.casc;

import static io.jenkins.plugins.casc.misc.Util.getJenkinsRoot;
import static io.jenkins.plugins.casc.misc.Util.toStringFromYamlFile;
import static io.jenkins.plugins.casc.misc.Util.toYamlString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import io.jenkins.plugins.casc.model.CNode;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@WithJenkinsConfiguredWithCode
class LDAPSecurityRealmTest {

    @Test
    @ConfiguredWithCode("LDAPSecurityRealmTestNoSecret.yml")
    void export_ldap_no_secret(JenkinsConfiguredWithCodeRule j) throws Exception {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        CNode yourAttribute =
                getJenkinsRoot(context).get("securityRealm").asMapping().get("ldap");

        String exported = toYamlString(yourAttribute);

        String expected = toStringFromYamlFile(this, "LDAPSecurityRealmTestNoSecretExpected.yml");

        assertThat(exported, is(expected));
    }
}
