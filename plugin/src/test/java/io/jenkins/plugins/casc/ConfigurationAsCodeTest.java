package io.jenkins.plugins.casc;

import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.util.FormValidation;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;

public class ConfigurationAsCodeTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    public void init_test_from_accepted_sources() throws Exception {
        ConfigurationAsCode casc = new ConfigurationAsCode();

        File exactFile = tempFolder.newFile("jenkins_1.yaml"); // expected

        tempFolder.newFile("jenkins_2.YAML"); // expected, alternate extension
        tempFolder.newFile("jenkins_3.YML");  // expected, alternate extension
        tempFolder.newFile("jenkins_4.yml"); // expected, alternate extension

        // should be picked up
        Path target = Paths.get("jenkins.tmp");
        Path newLink = Paths.get(tempFolder.getRoot().getAbsolutePath(), "jenkins_5.yaml");
        Files.createSymbolicLink(newLink, target);

        // should *NOT* be picked up
        tempFolder.newFolder("folder.yaml");

        // Replicate a k8s ConfigMap mount :
        // lrwxrwxrwx    1 root     root          19 Oct 15 16:43 jenkins_6.yaml -> ..data/jenkins_6.yaml
        // lrwxrwxrwx    1 root     root          31 Oct 29 16:29 ..data -> ..2018_10_29_16_29_08.094515936
        // drwxr-xr-x    2 root     root        4.0K Oct 29 16:29 ..2018_10_29_16_29_08.094515936

        final File timestamp = tempFolder.newFolder("..2018_10_29_16_29_08.094515936");
        new File(timestamp, "jenkins_6.yaml").createNewFile();
        final Path data = Paths.get(tempFolder.getRoot().getAbsolutePath(), "..data");
        Files.createSymbolicLink(data, timestamp.toPath());
        Files.createSymbolicLink(Paths.get(tempFolder.getRoot().getAbsolutePath(), "jenkins_6.yaml"),
                                 data.resolve("jenkins_6.yaml"));

        assertThat(casc.configs(exactFile.getAbsolutePath()), hasSize(1));
        final List<Path> foo = casc.configs(tempFolder.getRoot().getAbsolutePath());
        assertThat(foo, hasSize(5));
    }

    @Test(expected = ConfiguratorException.class)
    public void shouldReportMissingFileOnNotFoundConfig() throws ConfiguratorException {
        ConfigurationAsCode casc = new ConfigurationAsCode();
        casc.configure("some");
    }

    @Test
    @ConfiguredWithCode(value = {"merge1.yml", "merge3.yml"}, expected = ConfiguratorException.class)
    public void shouldMergeYamlConfig() {
        assertEquals("Configured by Configuration as Code plugin", j.jenkins.getSystemMessage());
        assertEquals(0, j.jenkins.getNumExecutors());
        assertNotNull(j.jenkins.getNode("agent1"));
        assertNotNull(j.jenkins.getNode("agent3"));
    }

    @Test
    @ConfiguredWithCode(value = {"merge1.yml", "merge2.yml"}, expected = ConfiguratorException.class)
    public void shouldReportConfigurationConflict() {
    }

    @Test
    public void doCheckNewSource_should_trim_input() throws Exception {
        ConfigurationAsCode casc = ConfigurationAsCode.get();

        String configUri = getClass().getResource("merge3.yml").toExternalForm();

        assertEquals(casc.doCheckNewSource("  " + configUri + "  ").kind, FormValidation.Kind.OK);
    }

    @Test
    public void doReplace_should_trim_input() throws Exception {
        HtmlPage page = j.createWebClient().goTo("configuration-as-code");
        j.assertGoodStatus(page);

        HtmlForm form = page.getFormByName("replace");
        HtmlInput input = form.getInputByName("_.newSource");
        String configUri = getClass().getResource("merge3.yml").toExternalForm();
        input.setValueAttribute("  " + configUri + "  ");
        HtmlPage resultPage = j.submit(form);
        j.assertGoodStatus(resultPage);

        assertEquals("Configured by Configuration as Code plugin", j.jenkins.getSystemMessage());
    }
}
