package io.jenkins.plugins.casc.misc.junit.jupiter;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.security.ACL;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.runner.Description;
import org.jvnet.hudson.test.JenkinsRecipe;

/**
 * Provides JUnit 5 compatibility for {@link JenkinsConfiguredWithCodeRule}.
 */
class JUnit5JenkinsConfiguredWithCodeRule extends JenkinsConfiguredWithCodeRule {

    JUnit5JenkinsConfiguredWithCodeRule(@NonNull ExtensionContext extensionContext, Annotation... annotations) {
        this.testDescription = Description.createTestDescription(
                extensionContext.getTestClass().map(Class::getName).orElse(null),
                extensionContext.getTestMethod().map(Method::getName).orElse(null),
                annotations);
    }

    @Override
    public void recipe() throws Exception {
        // so that test code has all the access to the system
        ACL.as2(ACL.SYSTEM2);

        final JenkinsRecipe recipe = this.testDescription.getAnnotation(JenkinsRecipe.class);
        if (recipe == null) {
            return;
        }
        @SuppressWarnings("unchecked")
        final JenkinsRecipe.Runner<JenkinsRecipe> runner = (JenkinsRecipe.Runner<JenkinsRecipe>)
                recipe.value().getDeclaredConstructor().newInstance();
        recipes.add(runner);
        tearDowns.add(() -> runner.tearDown(this, recipe));
    }
}
