package io.jenkins.plugins.casc.step;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.google.common.base.Charsets;
import hudson.EnvVars;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public class ConfigurationAsCodeBuilderTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();
    @ClassRule
    public static BuildWatcher bw = new BuildWatcher();

    @Before
    public void setup() throws Exception {
        TemporaryFolder root = createFolder(j.jenkins.root);
        addFile(root, "systemMessage.yml");
        addFile(root, "scmCheckoutRetryCount.yml");

        EnvironmentVariablesNodeProperty property = getEnvironmentVariables(root);
        j.jenkins.getGlobalNodeProperties().add(property);
    }

    @Test
    public void builder_shouldApplyConfigurationGivenFolderTarget() throws Exception {
        WorkflowJob project = j.createProject(WorkflowJob.class);
        project.setDefinition(new CpsFlowDefinition(readFile("folder.Jenkinsfile"), false));
        j.buildAndAssertSuccess(project);

        assertThat(j.jenkins.getSystemMessage(), is("Hello World!"));
        assertThat(j.jenkins.getScmCheckoutRetryCount(), is(5));
    }

    @Test
    public void builder_shouldApplyConfigurationGivenIndividualTargets() throws Exception {
        WorkflowJob project = j.createProject(WorkflowJob.class);
        project.setDefinition(new CpsFlowDefinition(readFile("individual.Jenkinsfile"), false));
        j.buildAndAssertSuccess(project);

        assertThat(j.jenkins.getSystemMessage(), is("Hello World!"));
        assertThat(j.jenkins.getScmCheckoutRetryCount(), is(5));
    }

    private TemporaryFolder createFolder(File base) throws Exception {
        TemporaryFolder root = new TemporaryFolder(base);
        root.create();
        return root;
    }

    private String readFile(String path) throws Exception {
        Path actual = Paths.get(getClass().getResource(path).toURI());
        return Files
                .lines(actual, Charsets.UTF_8)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private void addFile(TemporaryFolder root, String path) throws Exception {
        File file = root.newFile(path);
        Files.write(file.toPath(), readFile(path).getBytes());
    }

    private EnvironmentVariablesNodeProperty getEnvironmentVariables(TemporaryFolder root) {
        EnvironmentVariablesNodeProperty property = new EnvironmentVariablesNodeProperty();
        EnvVars vars = property.getEnvVars();
        vars.put("CASC_FOLDER", root.getRoot().toPath().toString());
        return property;
    }
}
