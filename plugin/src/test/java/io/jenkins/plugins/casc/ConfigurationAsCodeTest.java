package io.jenkins.plugins.casc;

import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.Functions;
import hudson.util.FormValidation;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.model.CNode;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule.WebClient;

import static com.gargoylesoftware.htmlunit.HttpMethod.POST;
import static io.jenkins.plugins.casc.ConfigurationAsCode.CASC_JENKINS_CONFIG_PROPERTY;
import static io.jenkins.plugins.casc.misc.Util.getJenkinsRoot;
import static io.jenkins.plugins.casc.misc.Util.toYamlString;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

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

        try {
            Files.createSymbolicLink(newLink, target);
        } catch (IOException e) {
            // often fails on windows due to non admin users not having symlink permission by default, see: https://stackoverflow.com/questions/23217460/how-to-create-soft-symbolic-link-using-java-nio-files/24353758#24353758
            Assume.assumeFalse(Functions.isWindows());
            throw e;
        }

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
        assertThat(foo, hasSize(6));
    }

    @Test
    public void test_ordered_config_loading() throws Exception {
        ConfigurationAsCode casc = new ConfigurationAsCode();

        tempFolder.newFile("0.yaml");
        tempFolder.newFile("1.yaml");
        tempFolder.newFile("a.yaml");
        tempFolder.newFile("z.yaml");

        final List<Path> foo = casc.configs(tempFolder.getRoot().getAbsolutePath());
        assertThat(foo, hasSize(4));
        assertTrue(foo.get(0).endsWith("0.yaml"));
        assertTrue(foo.get(1).endsWith("1.yaml"));
        assertTrue(foo.get(2).endsWith("a.yaml"));
        assertTrue(foo.get(3).endsWith("z.yaml"));
    }

    @Test
    public void test_loads_single_file_from_hidden_folder() throws Exception {
        ConfigurationAsCode casc = ConfigurationAsCode.get();

        File hiddenFolder = tempFolder.newFolder(".nested");
        File singleFile = new File(hiddenFolder, "jenkins.yml");
        try (InputStream configStream = getClass().getResourceAsStream("JenkinsConfigTest.yml")) {
            Files.copy(configStream, singleFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        String source = singleFile.toURI().toString();
        casc.configure(source);
        assertThat(casc.getSources(), contains(source));
        assertThat(j.jenkins.getSystemMessage(), equalTo("configuration as code - JenkinsConfigTest"));
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
        // expected to throw Configurator Exception
        // nodes should be empty due to conflict
        assertThat(j.jenkins.getNodes(), is(empty()));
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

    @Test
    @ConfiguredWithCode("admin.yml")
    public void doViewExport_should_require_authentication() throws Exception {
        WebClient client = j.createWebClient();
        WebRequest request =
            new WebRequest(client.createCrumbedUrl("configuration-as-code/viewExport"), POST);
        WebResponse response = client.loadWebResponse(request);
        assertThat(response.getStatusCode(), is(403));
        String user = "admin";
        WebClient loggedInClient = client.login(user, user);
        response = loggedInClient.loadWebResponse(request);
        assertThat(response.getStatusCode(), is(200));
    }

    @Test
    @Issue("Issue #739")
    public void preferEnvOverGlobalConfigForConfigPath() throws Exception {
        String firstConfig = getClass().getResource("JenkinsConfigTest.yml").toExternalForm();
        String secondConfig = getClass().getResource("merge3.yml").toExternalForm();
        CasCGlobalConfig descriptor = (CasCGlobalConfig) j.jenkins.getDescriptor(CasCGlobalConfig.class);
        assertNotNull(descriptor);
        descriptor.setConfigurationPath(firstConfig);
        ConfigurationAsCode.get().configure();
        assertThat(j.jenkins.getDescription(), is("configuration as code - JenkinsConfigTest"));
        System.setProperty(CASC_JENKINS_CONFIG_PROPERTY, secondConfig);
        ConfigurationAsCode.get().configure();
        assertThat(j.jenkins.getDescription(), is("Configured by Configuration as Code plugin"));
        System.clearProperty(CASC_JENKINS_CONFIG_PROPERTY);
    }

    @Test
    @ConfiguredWithCode(value = {"aNonEmpty.yml", "empty.yml"}) //file names matter for order!
    public void test_non_first_yaml_file_empty() {
        assertEquals("Configured by Configuration as Code plugin", j.jenkins.getSystemMessage());
    }

    @Test
    @Issue("Issue #914")
    public void isSupportedURI_should_not_throw_on_invalid_uri() {
        //for example, a Windows path is not a valid URI
        assertThat(ConfigurationAsCode.isSupportedURI("C:\\jenkins\\casc"), is(false));
    }

    @Test
    @ConfiguredWithCode("multi-line1.yml")
    public void multiline_literal_stays_literal_in_export() throws Exception {
        assertEquals("Welcome to our build server.\n\n"
                + "This Jenkins is 100% configured and managed 'as code'.\n",
            j.jenkins.getSystemMessage());

        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        CNode systemMessage = getJenkinsRoot(context).get("systemMessage");
        String exported = toYamlString(systemMessage);
        String expected = "|\n"
            + "  Welcome to our build server.\n\n"
            + "  This Jenkins is 100% configured and managed 'as code'.\n";
        assertThat(exported, is(expected));
    }

    @Test
    @ConfiguredWithCode("multi-line2.yml")
    public void string_to_literal_in_export() throws Exception {
        assertEquals("Welcome to our build server.\n\n"
                + "This Jenkins is 100% configured and managed 'as code'.\n",
            j.jenkins.getSystemMessage());

        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        CNode systemMessage = getJenkinsRoot(context).get("systemMessage");
        String exported = toYamlString(systemMessage);
        String expected = "|\n"
            + "  Welcome to our build server.\n\n"
            + "  This Jenkins is 100% configured and managed 'as code'.\n";
        assertThat(exported, is(expected));
    }
}
