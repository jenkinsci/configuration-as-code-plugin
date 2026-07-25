package io.jenkins.plugins.casc.core;

import static hudson.model.User.getById;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import hudson.model.User;
import hudson.security.HudsonPrivateSecurityRealm;
import hudson.util.Secret;
import io.jenkins.plugins.casc.ConfigurationAsCode;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import io.jenkins.plugins.casc.model.Sequence;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Objects;
import jenkins.security.ApiTokenProperty;
import org.junit.Rule;
import org.junit.Test;

public class HudsonPrivateSecurityRealmApiTokenTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("HudsonPrivateSecurityRealmConfiguratorTest_apiTokens.yml")
    public void configure_api_tokens() throws Exception {
        final User user = getById("tokenuser", false);
        assertNotNull("User 'tokenuser' should exist", user);

        ApiTokenProperty tokenProperty = user.getProperty(ApiTokenProperty.class);
        assertNotNull("User should have ApiTokenProperty", tokenProperty);

        Method getTokenListMethod = ApiTokenProperty.class.getMethod("getTokenList");
        Collection<?> tokenList = (Collection<?>) getTokenListMethod.invoke(tokenProperty);

        boolean hasTestToken = false;
        if (tokenList != null) {
            for (Object t : tokenList) {
                String tName;
                try {
                    tName = (String) t.getClass().getField("name").get(t);
                } catch (Exception e) {
                    tName = (String) t.getClass().getMethod("getName").invoke(t);
                }

                if ("test-token".equals(tName)) {
                    hasTestToken = true;
                    break;
                }
            }
        }

        assertTrue("User should have an API token named 'test-token'", hasTestToken);
    }

    @Test
    @ConfiguredWithCode("HudsonPrivateSecurityRealmConfiguratorTest_maskedTokens.yml")
    public void skip_masked_api_tokens() throws Exception {
        final User user = getById("maskeduser", false);
        assertNotNull("User 'maskeduser' should exist", user);

        ApiTokenProperty tokenProperty = user.getProperty(ApiTokenProperty.class);
        assertNotNull("User should have ApiTokenProperty", tokenProperty);

        Method getTokenListMethod = ApiTokenProperty.class.getMethod("getTokenList");
        Collection<?> tokenList = (Collection<?>) getTokenListMethod.invoke(tokenProperty);

        boolean hasMaskedToken = false;
        if (tokenList != null) {
            for (Object t : tokenList) {
                String tName;
                try {
                    tName = (String) t.getClass().getField("name").get(t);
                } catch (Exception e) {
                    tName = (String) t.getClass().getMethod("getName").invoke(t);
                }

                if ("masked-token".equals(tName)) {
                    hasMaskedToken = true;
                    break;
                }
            }
        }

        assertFalse("Masked tokens should be skipped and not added to the store", hasMaskedToken);
    }

    @Test
    @ConfiguredWithCode("HudsonPrivateSecurityRealmConfiguratorTest_apiTokens.yml")
    public void export_api_tokens() throws Exception {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        HudsonPrivateSecurityRealmConfigurator configurator = new HudsonPrivateSecurityRealmConfigurator();

        HudsonPrivateSecurityRealm realm = (HudsonPrivateSecurityRealm) j.jenkins.getSecurityRealm();

        CNode node = configurator.describe(realm, context);
        assertNotNull(node);

        Mapping realmMapping = node.asMapping();
        Sequence users = realmMapping.get("users").asSequence();

        Mapping userMapping = null;
        for (CNode userNode : users) {
            if ("tokenuser".equals(userNode.asMapping().getScalarValue("id"))) {
                userMapping = userNode.asMapping();
                break;
            }
        }
        assertNotNull("Exported users should include 'tokenuser'", userMapping);

        Sequence apiTokens = userMapping.get("apiTokens").asSequence();
        Mapping firstToken = apiTokens.get(0).asMapping();

        assertEquals("Exported token name should be 'test-token'", "test-token", firstToken.getScalarValue("name"));

        String exportedEncryptedToken = firstToken.getScalarValue("token");
        assertEquals(
                "Exported token value MUST be masked",
                "****",
                Secret.fromString(exportedEncryptedToken).getPlainText());
    }

    @Test
    @ConfiguredWithCode("HudsonPrivateSecurityRealmConfiguratorTest_apiTokens.yml")
    public void reapply_config_revokes_existing_tokens() throws Exception {

        String yamlResource = Objects.requireNonNull(
                        this.getClass().getResource("HudsonPrivateSecurityRealmConfiguratorTest_apiTokens.yml"))
                .toExternalForm();
        ConfigurationAsCode.get().configure(yamlResource);

        final User user = getById("tokenuser", false);
        assertNotNull(user);

        ApiTokenProperty tokenProperty = user.getProperty(ApiTokenProperty.class);
        assertNotNull(tokenProperty);

        Method getTokenListMethod = ApiTokenProperty.class.getMethod("getTokenList");
        Collection<?> tokenList = (Collection<?>) getTokenListMethod.invoke(tokenProperty);
        assertNotNull(tokenList);

        assertFalse("Token list should not be empty after revocation and re-adding", tokenList.isEmpty());
    }
}
