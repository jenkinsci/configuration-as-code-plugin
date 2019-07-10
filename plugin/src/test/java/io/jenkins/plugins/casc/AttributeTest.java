package io.jenkins.plugins.casc;

import hudson.util.Secret;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.For;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.LoggerRule;
import org.kohsuke.stapler.DataBoundConstructor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@For(Attribute.class)
public class AttributeTest {

    @Rule
    public LoggerRule logging = new LoggerRule();

    @Before
    public void tearUp() {
        logging.record(Logger.getLogger(Attribute.class.getName()), Level.FINEST).capture(2048);
    }

    @After
    public void tearDown() {
        for (String entry : logging.getMessages()) {
            System.out.println(entry);
        }
    }

    @Test
    @Issue("SECURITY-1279")
    public void checkCommonSecretPatterns() {
        assertFieldIsSecret(WellDefinedField.class, "secretField");
        assertFieldIsSecret(SecretFromGetter.class, "secretField");
        assertFieldIsSecret(SecretFromPublicField.class, "secretField");
        assertFieldIsSecret(SecretFromPrivateField.class, "secretField");

        assertFieldIsSecret(SecretRenamedFieldFithSecretConstructor.class, "mySecretValueField");
    }

    @Test
    @Issue("SECURITY-1279")
    public void checkStaticResolution() {
        // Field is not secret, but the attribute is secret
        assertFieldIsNotSecret(SecretRenamedFieldFithSecretConstructor.class, "mySecretValue");
    }

    @Test
    @Issue("SECURITY-1279")
    public void checkCommonSecretPatternsForOverrides() {
        assertFieldIsSecret(WellDefinedField2.class, "secret");
        assertFieldIsSecret(SecretFromGetter2.class, "secretField");

        // Fields
        assertFieldIsSecret(SecretFromPublicField2.class, "secretField");
        assertFieldIsSecret(SecretFromPrivateField2.class, "secretField");
        assertFieldIsSecret(SecretFromPrivateField3.class, "secretField");
    }

    @Test
    @Issue("SECURITY-1279")
    public void checkNonSecretPatterns() {
        assertFieldIsNotSecret(NonSecretField.class, "passwordPath");
    }

    public static void assertFieldIsSecret(Class<?> clazz, String fieldName) {
        String displayName = clazz != null ? (clazz.getName() + "#" + fieldName) : fieldName;
        assertTrue("Field is not secret: " + displayName,
                Attribute.calculateIfSecret(clazz, fieldName));
    }

    public static void assertFieldIsNotSecret(Class<?> clazz, String fieldName) {
        String displayName = clazz != null ? (clazz.getName() + "#" + fieldName) : fieldName;
        assertFalse("Field is a secret while it should not be considered as one: " + displayName,
                Attribute.calculateIfSecret(clazz, fieldName));
    }

    public static class WellDefinedField {

        Secret secretField;

        @DataBoundConstructor
        public WellDefinedField(Secret secretField) {
            this.secretField = secretField;
        }

        public Secret getSecret() {
            return secretField;
        }
    }

    public static class WellDefinedField2 extends WellDefinedField {
        public WellDefinedField2(Secret secret) {
            super(secret);
        }
    }

    public static class SecretFromGetter {

        Secret secretField;

        @DataBoundConstructor
        public SecretFromGetter(String secretField) {
            this.secretField = Secret.fromString(secretField);
        }

        public Secret getSecret() {
            return secretField;
        }
    }

    public static class SecretFromGetter2 extends SecretFromGetter {
        public SecretFromGetter2(String secret) {
            super(secret);
        }
    }

    public static class SecretFromPublicField {

        public Secret secretField;

        @DataBoundConstructor
        public SecretFromPublicField(String secretField) {
            this.secretField = Secret.fromString(secretField);
        }
    }

    public static class SecretFromPublicField2 extends SecretFromPublicField {
        public SecretFromPublicField2(String secret) {
            super(secret);
        }
    }

    public static class SecretFromPrivateField {

        private Secret secretField;

        @DataBoundConstructor
        public SecretFromPrivateField(String secretField) {
            this.secretField = Secret.fromString(secretField);
        }
    }

    public static class SecretFromPrivateField2 extends SecretFromPrivateField {
        public SecretFromPrivateField2(String secret) {
            super(secret);
        }
    }

    public static class SecretFromPrivateField3 extends SecretFromPrivateField2 {
        public SecretFromPrivateField3(String secret) {
            super(secret);
        }
    }

    public static class NonSecretField {

        public String passwordPath;

        @DataBoundConstructor
        public NonSecretField(String passwordPath) {
            this.passwordPath = passwordPath;
        }
    }

    public static class SecretRenamedFieldFithSecretConstructor {

        Secret mySecretValueField;

        @DataBoundConstructor
        public SecretRenamedFieldFithSecretConstructor(Secret mySecretValue) {
            this.mySecretValueField = mySecretValue;
        }

        public String getMySecretValue() {
            return mySecretValueField.getPlainText();
        }
    }
}
