package io.jenkins.plugins.casc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.util.Secret;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.For;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.LogRecorder;
import org.kohsuke.stapler.DataBoundConstructor;

@For(Attribute.class)
public class AttributeTest {

    private LogRecorder logging;

    @BeforeEach
    void tearUp() {
        logging = new LogRecorder()
                .record(Logger.getLogger(Attribute.class.getName()), Level.FINEST)
                .capture(2048);
    }

    @AfterEach
    void tearDown() {
        for (String entry : logging.getMessages()) {
            System.out.println(entry);
        }
        logging.close();
    }

    @Test
    @Issue("SECURITY-1279")
    void checkCommonSecretPatterns() {
        assertFieldIsSecret(WellDefinedField.class, "secretField");
        assertFieldIsSecret(SecretFromGetter.class, "secretField");
        assertFieldIsSecret(SecretFromPublicField.class, "secretField");
        assertFieldIsSecret(SecretFromPrivateField.class, "secretField");

        assertFieldIsSecret(SecretRenamedFieldFithSecretConstructor.class, "mySecretValueField");
    }

    @Test
    @Issue("SECURITY-1279")
    void checkStaticResolution() {
        // Field is not secret, but the attribute is secret
        assertFieldIsNotSecret(SecretRenamedFieldFithSecretConstructor.class, "mySecretValue");
    }

    @Test
    @Issue("SECURITY-1279")
    void checkCommonSecretPatternsForOverrides() {
        assertFieldIsSecret(WellDefinedField2.class, "secret");
        assertFieldIsSecret(SecretFromGetter2.class, "secretField");

        // Fields
        assertFieldIsSecret(SecretFromPublicField2.class, "secretField");
        assertFieldIsSecret(SecretFromPrivateField2.class, "secretField");
        assertFieldIsSecret(SecretFromPrivateField3.class, "secretField");
    }

    @Test
    @Issue("SECURITY-1279")
    void checkNonSecretPatterns() {
        assertFieldIsNotSecret(NonSecretField.class, "passwordPath");
    }

    @Test
    void checkSecretFromSetter() {
        assertFieldIsSecret(SecretFromSetter.class, "secretField");
    }

    @Test
    void checkUnknownClassCondition() {
        assertFalse(
                Attribute.calculateIfSecret(null, "someField"),
                "calculateIfSecret should return false when targetClass is unknown");
    }

    public static void assertFieldIsSecret(Class<?> clazz, String fieldName) {
        String displayName = clazz != null ? (clazz.getName() + "#" + fieldName) : fieldName;
        assertTrue(Attribute.calculateIfSecret(clazz, fieldName), "Field should be a secret: " + displayName);
    }

    public static void assertFieldIsNotSecret(Class<?> clazz, String fieldName) {
        String displayName = clazz != null ? (clazz.getName() + "#" + fieldName) : fieldName;
        assertFalse(Attribute.calculateIfSecret(clazz, fieldName), "Field should not be a secret: " + displayName);
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

        private final String hiddenData;

        @DataBoundConstructor
        public SecretFromGetter(String secretField) {
            this.hiddenData = secretField;
        }

        @SuppressWarnings("unused")
        public Secret getSecretField() {
            return Secret.fromString(hiddenData);
        }
    }

    public static class SecretFromGetter2 extends SecretFromGetter {
        public SecretFromGetter2(String secretField) {
            super(secretField);
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

    public static class SecretFromSetter {

        private String secretField;

        @DataBoundConstructor
        public SecretFromSetter() {}

        @SuppressWarnings("unused")
        public String getSecretField() {
            return secretField;
        }

        @SuppressWarnings("unused")
        public void setSecretField(Secret secretField) {
            this.secretField = secretField.getPlainText();
        }
    }

    @Test
    void checkCalculationIsIdempotent() {
        boolean firstTrue = Attribute.calculateIfSecret(SecretFromSetter.class, "secretField");
        boolean secondTrue = Attribute.calculateIfSecret(SecretFromSetter.class, "secretField");
        assertTrue(firstTrue);
        assertTrue(secondTrue, "Subsequent calls should return the same TRUE result");

        boolean firstFalse = Attribute.calculateIfSecret(NonSecretField.class, "passwordPath");
        boolean secondFalse = Attribute.calculateIfSecret(NonSecretField.class, "passwordPath");
        assertFalse(firstFalse);
        assertFalse(secondFalse, "Subsequent calls should return the same FALSE result");

        boolean firstUnknown = Attribute.calculateIfSecret(null, "someField");
        boolean secondUnknown = Attribute.calculateIfSecret(null, "someField");
        assertFalse(firstUnknown);
        assertFalse(secondUnknown, "Subsequent calls should return the same fallback FALSE result");
    }
}
