package io.jenkins.plugins.casc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.ExtensionComponent;
import hudson.ExtensionFinder;
import hudson.model.Hudson;
import io.jenkins.plugins.casc.encryption.DecryptionEngine;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import jenkins.ExtensionComponentSet;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
public class DecryptionEngineTest {

    @Test
    void rejectsDuplicateCaseInsensitiveIds(JenkinsRule j) {
        registerEngines(j, new StubEngine("dummy-dup"), new StubEngine("DUMMY-DUP"));

        IllegalStateException ex = assertThrows(IllegalStateException.class, DecryptionEngine::all);
        assertTrue(ex.getMessage().contains("Duplicate decryption engine ID"));
    }

    @Test
    void allowsDifferentIds(JenkinsRule j) {
        DecryptionEngine engine1 = new StubEngine("dummy-engine-1");
        DecryptionEngine engine2 = new StubEngine("dummy-engine-2");

        registerEngines(j, engine1, engine2);
        var engines = DecryptionEngine.all();

        assertTrue(engines.size() >= 2, "Should resolve at least our two dummy engines");
        assertTrue(engines.contains(engine1));
        assertTrue(engines.contains(engine2));
    }

    @Test
    void rejectsBlankId(JenkinsRule j) {
        registerEngines(j, new StubEngine("   "));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                DecryptionEngine::all,
                "Engine with blank ID should fail during resolution");
        assertTrue(ex.getMessage().contains("has a null or blank ID"));
    }

    @Test
    void rejectsNullId(JenkinsRule j) {
        registerEngines(j, new StubEngine(null));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                DecryptionEngine::all,
                "Engine with null ID should fail during resolution");
        assertTrue(ex.getMessage().contains("has a null or blank ID"));
    }

    @Test
    void contractViolationWhenEngineReturnsNull(JenkinsRule j) {
        DecryptionEngine nullReturningEngine = new StubEngine("dummy-null-test") {
            @Override
            public String decrypt(String encryptedData) {
                return null;
            }
        };
        registerEngines(j, nullReturningEngine);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> DecryptLookup.INSTANCE.lookup("dummy-null-test:some-secret-data"),
                "DecryptLookup should throw an exception if the underlying engine returns null");

        assertTrue(ex.getMessage().contains("returned null"));
    }

    @Test
    void exceptionMessageConfidentiality(JenkinsRule j) {
        String sensitiveData = "SUPER_SECRET_PLAINTEXT_DO_NOT_LEAK";

        DecryptionEngine leakingEngine = new StubEngine("dummy-leak-test") {
            @Override
            public String decrypt(String encryptedData) throws GeneralSecurityException {
                throw new GeneralSecurityException("Failed to decrypt: " + sensitiveData);
            }
        };
        registerEngines(j, leakingEngine);

        Exception ex = assertThrows(
                Exception.class, () -> DecryptLookup.INSTANCE.lookup("dummy-leak-test:some-encrypted-payload"));

        assertFalse(
                ex.getMessage().contains(sensitiveData),
                "Security Violation: DecryptLookup leaked sensitive plaintext in the exception message!");
    }

    @Test
    void exceptionIsPropagatedOnFailure(JenkinsRule j) {
        DecryptionEngine failingEngine = new StubEngine("dummy-fail-test") {
            @Override
            public String decrypt(String encryptedData) throws GeneralSecurityException {
                throw new GeneralSecurityException("Invalid payload");
            }
        };
        registerEngines(j, failingEngine);

        DecryptionEngine resolved = DecryptionEngine.all().stream()
                .filter(e -> "dummy-fail-test".equals(e.getId()))
                .findFirst()
                .orElseThrow();

        GeneralSecurityException ex = assertThrows(GeneralSecurityException.class, () -> resolved.decrypt("bad-data"));
        assertEquals("Invalid payload", ex.getMessage());
    }

    private void registerEngines(JenkinsRule j, DecryptionEngine... engines) {
        ExtensionFinder finder = new ExtensionFinder() {
            @Override
            public <T> Collection<ExtensionComponent<T>> find(Class<T> type, Hudson hudson) {
                List<ExtensionComponent<T>> result = new ArrayList<>();
                if (type == DecryptionEngine.class) {
                    for (DecryptionEngine engine : engines) {
                        result.add(new ExtensionComponent<>(type.cast(engine)));
                    }
                }
                return result;
            }

            @Override
            public ExtensionComponentSet refresh() {
                return new ExtensionComponentSet() {
                    @Override
                    public <T> Collection<ExtensionComponent<T>> find(Class<T> type) {
                        return Collections.emptyList();
                    }
                };
            }
        };

        j.jenkins.getExtensionList(ExtensionFinder.class).add(0, finder);

        try {
            j.jenkins.refreshExtensions();
        } catch (Exception e) {
            throw new RuntimeException("Failed to refresh extensions", e);
        }
    }

    private static class StubEngine extends DecryptionEngine {
        private final String id;

        StubEngine(String id) {
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String decrypt(String encryptedData) throws GeneralSecurityException {
            return "decrypted-" + encryptedData;
        }
    }
}
