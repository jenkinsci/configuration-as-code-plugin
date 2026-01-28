package io.jenkins.plugins.casc;

import static io.jenkins.plugins.casc.ConfigurationAsCode.CASC_JENKINS_CONFIG_PROPERTY;
import static io.jenkins.plugins.casc.misc.Util.getJenkinsRoot;
import static io.jenkins.plugins.casc.misc.Util.toYamlString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.htmlunit.HttpMethod.POST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import hudson.Functions;
import hudson.util.FormValidation;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Source;
import io.jenkins.plugins.casc.yaml.YamlSource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.htmlunit.WebRequest;
import org.htmlunit.WebResponse;
import org.htmlunit.html.HtmlButton;
import org.htmlunit.html.HtmlElementUtil;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlInput;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule.WebClient;

@WithJenkinsConfiguredWithCode
class ConfigurationAsCodeTest {

    @TempDir
    public File tempFolder;

    @Test
    void init_test_from_accepted_sources(JenkinsConfiguredWithCodeRule j) throws Exception {
        ConfigurationAsCode casc = new ConfigurationAsCode();

        File exactFile = newFile(tempFolder, "jenkins_1.yaml"); // expected

        newFile(tempFolder, "jenkins_2.YAML"); // expected, alternate extension
        newFile(tempFolder, "jenkins_3.YML"); // expected, alternate extension
        newFile(tempFolder, "jenkins_4.yml"); // expected, alternate extension

        // should be picked up
        Path target = Paths.get("jenkins.tmp");
        Path newLink = Paths.get(tempFolder.getAbsolutePath(), "jenkins_5.yaml");

        try {
            Files.createSymbolicLink(newLink, target);
        } catch (IOException e) {
            // often fails on windows due to non admin users not having symlink permission by default, see:
            // https://stackoverflow.com/questions/23217460/how-to-create-soft-symbolic-link-using-java-nio-files/24353758#24353758
            assumeFalse(Functions.isWindows());
            throw e;
        }

        // should *NOT* be picked up
        newFolder(tempFolder, "folder.yaml");

        // Replicate a k8s ConfigMap mount :
        // lrwxrwxrwx    1 root     root          19 Oct 15 16:43 jenkins_6.yaml -> ..data/jenkins_6.yaml
        // lrwxrwxrwx    1 root     root          31 Oct 29 16:29 ..data -> ..2018_10_29_16_29_08.094515936
        // drwxr-xr-x    2 root     root        4.0K Oct 29 16:29 ..2018_10_29_16_29_08.094515936

        final File timestamp = newFolder(tempFolder, "..2018_10_29_16_29_08.094515936");
        new File(timestamp, "jenkins_6.yaml").createNewFile();
        final Path data = Paths.get(tempFolder.getAbsolutePath(), "..data");
        Files.createSymbolicLink(data, timestamp.toPath());
        Files.createSymbolicLink(
                Paths.get(tempFolder.getAbsolutePath(), "jenkins_6.yaml"), data.resolve("jenkins_6.yaml"));

        // Create a symbolic linked folder with 1 configuration file
        final File folderLinkTarget = newFolder(tempFolder, "..2019_11_26_16_29_08.094515937");
        new File(folderLinkTarget, "jenkins_7.yaml").createNewFile();
        Files.createSymbolicLink(Paths.get(tempFolder.getAbsolutePath(), "linked_folder"), folderLinkTarget.toPath());

        assertThat(casc.configs(exactFile.getAbsolutePath()), hasSize(1));
        final List<Path> foo = casc.configs(tempFolder.getAbsolutePath());
        assertThat("failed to find 7 configs in " + Arrays.toString(tempFolder.list()), foo, hasSize(7));
    }

    @Test
    void test_ordered_config_loading(JenkinsConfiguredWithCodeRule j) throws Exception {
        ConfigurationAsCode casc = new ConfigurationAsCode();

        newFile(tempFolder, "0.yaml");
        newFile(tempFolder, "1.yaml");
        newFile(tempFolder, "a.yaml");
        newFile(tempFolder, "z.yaml");

        final List<Path> foo = casc.configs(tempFolder.getAbsolutePath());
        assertThat("failed to find 4 configs in " + Arrays.toString(tempFolder.list()), foo, hasSize(4));
        assertTrue(foo.get(0).endsWith("0.yaml"));
        assertTrue(foo.get(1).endsWith("1.yaml"));
        assertTrue(foo.get(2).endsWith("a.yaml"));
        assertTrue(foo.get(3).endsWith("z.yaml"));
    }

    @Test
    void test_loads_single_file_from_hidden_folder(JenkinsConfiguredWithCodeRule j) throws Exception {
        ConfigurationAsCode casc = ConfigurationAsCode.get();

        File hiddenFolder = newFolder(tempFolder, ".nested");
        File singleFile = new File(hiddenFolder, "jenkins.yml");
        try (InputStream configStream = getClass().getResourceAsStream("JenkinsConfigTest.yml")) {
            Files.copy(configStream, singleFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        String source = singleFile.toURI().toString();
        casc.configure(source);
        assertThat(casc.getSources(), contains(source));
        assertThat(j.jenkins.getSystemMessage(), equalTo("configuration as code - JenkinsConfigTest"));
    }

    @Test
    @ConfiguredWithCode(
            value = {"merge1.yml", "merge3.yml"},
            expected = ConfiguratorException.class)
    void test_loads_multi_files(JenkinsConfiguredWithCodeRule j) {
        ConfigurationAsCode casc = ConfigurationAsCode.get();

        List<String> sources = casc.getSources();
        assertNotNull(sources);
        assertEquals(2, sources.size());
    }

    @Test
    void shouldReportMissingFileOnNotFoundConfig() {
        ConfigurationAsCode casc = new ConfigurationAsCode();
        assertThrows(ConfiguratorException.class, () -> casc.configure("some"));
    }

    @Test
    @ConfiguredWithCode(
            value = {"merge1.yml", "merge3.yml"},
            expected = ConfiguratorException.class)
    void shouldMergeYamlConfig(JenkinsConfiguredWithCodeRule j) {
        assertEquals("Configured by Configuration as Code plugin", j.jenkins.getSystemMessage());
        assertEquals(0, j.jenkins.getNumExecutors());
        assertNotNull(j.jenkins.getNode("agent1"));
        assertNotNull(j.jenkins.getNode("agent3"));
    }

    @Test
    @ConfiguredWithCode(
            value = {"merge1.yml", "merge2.yml"},
            expected = ConfiguratorException.class)
    void shouldReportConfigurationConflict(JenkinsConfiguredWithCodeRule j) {
        // expected to throw Configurator Exception
        // nodes should be empty due to conflict
        assertThat(j.jenkins.getNodes(), is(empty()));
    }

    @Test
    void doCheckNewSource_should_trim_input(JenkinsConfiguredWithCodeRule j) {
        ConfigurationAsCode casc = ConfigurationAsCode.get();

        String configUri = getClass().getResource("merge3.yml").toExternalForm();

        assertEquals(FormValidation.Kind.OK, casc.doCheckNewSource("  " + configUri + "  ").kind);
    }

    @Test
    void doCheckNewSource_should_support_multiple_sources(JenkinsConfiguredWithCodeRule j) {
        ConfigurationAsCode casc = ConfigurationAsCode.get();

        String configUri = String.format(
                " %s , %s ",
                getClass().getResource("JenkinsConfigTest.yml").toExternalForm(),
                getClass().getResource("folder/jenkins2.yml").toExternalForm());

        assertEquals(FormValidation.Kind.OK, casc.doCheckNewSource(configUri).kind);
    }

    @Test
    @Issue("Issue #653")
    void checkWith_should_pass_against_valid_input(JenkinsConfiguredWithCodeRule j) throws Exception {
        String rawConf = getClass().getResource("JenkinsConfigTest.yml").toExternalForm();
        YamlSource input = YamlSource.of(rawConf);
        Map<Source, String> actual = ConfigurationAsCode.get().checkWith(input);
        assertThat(actual.size(), is(0));
    }

    @Test
    @Issue("Issue #653")
    @ConfiguredWithCode("aNonEmpty.yml")
    void checkWith_should_pass_against_input_which_has_same_entries_with_initial_config(JenkinsConfiguredWithCodeRule j)
            throws Exception {
        String rawConf = getClass().getResource("JenkinsConfigTest.yml").toExternalForm();
        YamlSource input = YamlSource.of(rawConf);
        Map<Source, String> actual = ConfigurationAsCode.get().checkWith(input);
        assertThat(actual.size(), is(0));
    }

    @Test
    void doReplace_should_trim_input(JenkinsConfiguredWithCodeRule j) throws Exception {
        HtmlPage page = j.createWebClient().goTo("configuration-as-code");
        j.assertGoodStatus(page);

        HtmlButton button = (HtmlButton) page.getElementById("btn-open-apply-configuration");
        HtmlElementUtil.click(button);

        HtmlForm form = page.getFormByName("replace");
        HtmlInput input = form.getInputByName("_.newSource");
        String configUri = getClass().getResource("merge3.yml").toExternalForm();
        input.setValue("  " + configUri + "  ");
        HtmlPage resultPage = j.submit(form);
        j.assertGoodStatus(resultPage);

        assertEquals("Configured by Configuration as Code plugin", j.jenkins.getSystemMessage());
    }

    @Test
    void doReplace_should_support_multiple_sources(JenkinsConfiguredWithCodeRule j) throws Exception {
        HtmlPage page = j.createWebClient().goTo("configuration-as-code");
        j.assertGoodStatus(page);

        HtmlButton button = (HtmlButton) page.getElementById("btn-open-apply-configuration");
        HtmlElementUtil.click(button);

        HtmlForm form = page.getFormByName("replace");
        HtmlInput input = form.getInputByName("_.newSource");
        String configUri = String.format(
                " %s , %s ",
                getClass().getResource("JenkinsConfigTest.yml").toExternalForm(),
                getClass().getResource("folder/jenkins2.yml").toExternalForm());
        input.setValue(configUri);
        HtmlPage resultPage = j.submit(form);
        j.assertGoodStatus(resultPage);

        assertEquals("configuration as code - JenkinsConfigTest", j.jenkins.getSystemMessage());
        assertEquals(10, j.jenkins.getQuietPeriod());
    }

    @Test
    @ConfiguredWithCode("admin.yml")
    void doViewExport_should_require_authentication(JenkinsConfiguredWithCodeRule j) throws Exception {
        WebClient client = j.createWebClient();
        WebRequest request = new WebRequest(client.createCrumbedUrl("configuration-as-code/viewExport"), POST);
        WebResponse response = client.loadWebResponse(request);
        assertThat(response.getStatusCode(), is(403));
        String user = "admin";
        WebClient loggedInClient = client.login(user, user);
        response = loggedInClient.loadWebResponse(request);
        assertThat(response.getStatusCode(), is(200));
    }

    @Test
    @Issue("Issue #739")
    void preferEnvOverGlobalConfigForConfigPath(JenkinsConfiguredWithCodeRule j) throws Exception {
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

    // file names matter for order!
    @Test
    @ConfiguredWithCode(value = {"aNonEmpty.yml", "empty.yml"})
    void test_non_first_yaml_file_empty(JenkinsConfiguredWithCodeRule j) {
        assertEquals("Configured by Configuration as Code plugin", j.jenkins.getSystemMessage());
    }

    @Test
    @Issue("Issue #914")
    void isSupportedURI_should_not_throw_on_invalid_uri() {
        // for example, a Windows path is not a valid URI
        assertThat(ConfigurationAsCode.isSupportedURI("C:\\jenkins\\casc"), is(false));
    }

    @Test
    @ConfiguredWithCode("multi-line1.yml")
    void multiline_literal_stays_literal_in_export(JenkinsConfiguredWithCodeRule j) throws Exception {
        assertEquals(
                "Welcome to our build server.\n\n" + "This Jenkins is 100% configured and managed 'as code'.\n",
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
    void string_to_literal_in_export(JenkinsConfiguredWithCodeRule j) throws Exception {
        assertEquals(
                "Welcome to our build server.\n\n" + "This Jenkins is 100% configured and managed 'as code'.\n",
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
    void testHtmlDocStringRetrieval(JenkinsConfiguredWithCodeRule j) throws Exception {
        String expectedDocString = "<div>\n"
                + "  If checked, this will allow users who are not authenticated to access Jenkins\n  in a read-only mode.\n"
                + "</div>\n";
        String actualDocString = ConfigurationAsCode.get()
                .getHtmlHelp(hudson.security.FullControlOnceLoggedInAuthorizationStrategy.class, "allowAnonymousRead");
        assertEquals(expectedDocString, actualDocString);
    }

    @Test
    void configurationCategory(JenkinsConfiguredWithCodeRule j) {
        ConfigurationAsCode configurationAsCode = ConfigurationAsCode.get();
        assertThat(configurationAsCode.getCategoryName(), is("CONFIGURATION"));
    }

    private static File newFolder(File root, String... subDirs) throws IOException {
        String subFolder = String.join("/", subDirs);
        File result = new File(root, subFolder);
        if (!result.mkdirs()) {
            throw new IOException("Couldn't create folders " + result);
        }
        return result;
    }

    private static File newFile(File root, String fileName) throws IOException {
        File result = new File(root, fileName);
        if (!result.createNewFile()) {
            throw new IOException("Couldn't create file " + result);
        }
        return result;
    }
}
