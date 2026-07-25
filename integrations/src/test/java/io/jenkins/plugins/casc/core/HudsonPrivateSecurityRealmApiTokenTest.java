package io.jenkins.plugins.casc.core;

import static hudson.model.User.getById;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import hudson.model.User;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import java.lang.reflect.Method;
import java.util.Collection;
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
}
