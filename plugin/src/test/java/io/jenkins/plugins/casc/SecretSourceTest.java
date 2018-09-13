package io.jenkins.plugins.casc;

import org.junit.Test;

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


}
