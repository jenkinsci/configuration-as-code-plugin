package io.jenkins.plugins.casc.misc.junit.jupiter;

import static org.junit.platform.commons.support.ReflectionSupport.findFields;

import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import java.lang.reflect.Field;
import java.util.function.Predicate;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContextException;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.junit.platform.commons.support.ModifierSupport;

/**
 * JUnit 5 extension providing {@link JenkinsConfiguredWithCodeRule} integration.
 *
 * @see WithJenkinsConfiguredWithCode
 */
class JenkinsConfiguredWithCodeExtension
        implements BeforeAllCallback, AfterAllCallback, ParameterResolver, AfterEachCallback {

    private static final String KEY = "jenkins-instance";
    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(JenkinsConfiguredWithCodeExtension.class);

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        Class<?> clazz = extensionContext.getRequiredTestClass();
        Predicate<Field> predicate = (field -> ModifierSupport.isStatic(field)
                && JenkinsConfiguredWithCodeRule.class.isAssignableFrom(field.getType()));
        Field field = findFields(clazz, predicate, HierarchyTraversalMode.BOTTOM_UP).stream()
                .findFirst()
                .orElse(null);
        if (field == null) {
            return;
        }

        final JenkinsConfiguredWithCodeRule rule =
                new JUnit5JenkinsConfiguredWithCodeRule(extensionContext, field.getDeclaredAnnotations());

        extensionContext
                .getStore(NAMESPACE)
                .getOrComputeIfAbsent(KEY, key -> rule, JenkinsConfiguredWithCodeRule.class);
        try {
            rule.before();
        } catch (Throwable e) {
            throw new ExtensionContextException(e.getMessage(), e);
        }
        field.setAccessible(true);
        field.set(null, rule);
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        Class<?> clazz = extensionContext.getRequiredTestClass();
        Predicate<Field> predicate = (field -> ModifierSupport.isStatic(field)
                && JenkinsConfiguredWithCodeRule.class.isAssignableFrom(field.getType()));
        Field field = findFields(clazz, predicate, HierarchyTraversalMode.BOTTOM_UP).stream()
                .findFirst()
                .orElse(null);
        if (field != null) {
            final JenkinsConfiguredWithCodeRule rule =
                    extensionContext.getStore(NAMESPACE).get(KEY, JenkinsConfiguredWithCodeRule.class);
            if (rule == null) {
                return;
            }
            rule.after();
        }
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        final JenkinsConfiguredWithCodeRule rule =
                context.getStore(NAMESPACE).remove(KEY, JenkinsConfiguredWithCodeRule.class);
        if (rule == null) {
            return;
        }
        rule.after();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return parameterContext.getParameter().getType().equals(JenkinsConfiguredWithCodeRule.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {

        final JenkinsConfiguredWithCodeRule rule = extensionContext
                .getStore(NAMESPACE)
                .getOrComputeIfAbsent(
                        KEY,
                        key -> new JUnit5JenkinsConfiguredWithCodeRule(
                                extensionContext,
                                extensionContext.getRequiredTestMethod().getDeclaredAnnotations()),
                        JenkinsConfiguredWithCodeRule.class);

        try {
            rule.before();
            return rule;
        } catch (Throwable t) {
            throw new ParameterResolutionException(t.getMessage(), t);
        }
    }
}
