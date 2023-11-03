package io.jenkins.plugins.casc.misc.junit5;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.runner.Description;
import org.jvnet.hudson.test.JenkinsRecipe;

import java.lang.reflect.Method;

/**
 * Provides JUnit 5 compatibility for {@link JenkinsConfiguredWithCodeRule}.
 */
class JUnit5JenkinsConfiguredWithCodeRule extends JenkinsConfiguredWithCodeRule {
    private JenkinsRecipe recipe;

    JUnit5JenkinsConfiguredWithCodeRule(@NonNull ExtensionContext extensionContext) {
        this.testDescription = Description.createTestDescription(
                extensionContext.getTestClass().map(Class::getName).orElse(null),
                extensionContext.getTestMethod().map(Method::getName).orElse(null));
    }


    JUnit5JenkinsConfiguredWithCodeRule(@NonNull ExtensionContext extensionContext, JenkinsRecipe recipe) {
        this.recipe = recipe;
        this.testDescription = Description.createTestDescription(
            extensionContext.getTestClass().map(Class::getName).orElse(null),
            extensionContext.getTestMethod().map(Method::getName).orElse(null));
    }

    @Override
    public void recipe() throws Exception {
        if (recipe == null) {
            return;
        }
        @SuppressWarnings("unchecked")
        final JenkinsRecipe.Runner<JenkinsRecipe> runner =
                (JenkinsRecipe.Runner<JenkinsRecipe>) recipe.value().getDeclaredConstructor().newInstance();
        recipes.add(runner);
        tearDowns.add(() -> runner.tearDown(this, recipe));
    }
}
