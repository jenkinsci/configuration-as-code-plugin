package io.jenkins.plugins.casc.misc;

import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import io.jenkins.plugins.casc.ConfigurationAsCode;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import io.jenkins.plugins.casc.model.Scalar;
import io.jenkins.plugins.casc.model.Sequence;
import io.jenkins.plugins.casc.yaml.YamlSource;
import io.jenkins.plugins.casc.yaml.YamlUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.LoggerRule;
import org.jvnet.hudson.test.RestartableJenkinsRule;

import static com.gargoylesoftware.htmlunit.HttpMethod.POST;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Base test to check a complete test of each plugin configuration. What it makes:
 * 1.  Configure the instance with the {@link #configResource()} implemented.
 * 2.  Check it was configured correctly.
 * 3.  Export the full Jenkins configuration via Web UI (hacked as it fails so far).
 * 4.  Export the schema via Web UI (commented out as there is no good validator so far).
 * 5.  Verify the Jenkins configuration against the schema (commented out as there is no good validator so far).
 * 6.  Check the Jenkins configuration is valid via Web UI (used the plugin config so far).
 * 7.  Apply the Jenkins configuration via Web UI (maybe not needed, but we test it as well).
 * 8.  Write the Jenkins configuration to $JENKINS_ROOT/jenkins.yaml.
 * 9.  Restart Jenkins.
 * 10. Check the {@link #stringInLogExpected()} is set during the restart.
 *
 * All the plugin author needs to do is override the methods providing:
 * 1. The resource with the yaml configuration of the plugin in case they use their own name for the file
 * 2. A way to validate the configuration is established
 * 3. A string that should be present in the logs that guarantees the config is loaded. Usually a weird text configured.
 */
public abstract class ExportImportRoundTripAbstractTest {
    @Rule
    public RestartableJenkinsRule r = new RestartableJenkinsRule();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public LoggerRule logging = new LoggerRule();

    /**
     * A method to assert if the configuration was correctly loaded. The Jenkins rule and the content of the config
     * supposedly loaded are passed.
     */
    protected abstract void assertConfiguredAsExpected(RestartableJenkinsRule j, String configContent);

    /**
     * Return the resource path (yaml file) to be loaded. i.e: If the resource is in the same package of the implementor
     * class, then: my-config.yaml
     * @return the resource name and path.
     */
    protected String configResource() {
        return "configuration-as-code.yaml";
    }

    /**
     * Return the string that should be in the logs of the JCasC logger to verify it's configured after a restart. This
     * string should be unique to avoid interpreting that it was configured successfully, but it wasn't.
     * @return the unique string to be in the logs to certify the configuration was done successfully.
     */
    protected abstract String stringInLogExpected();

    /**
     * 1.  Configure the instance with the {@link #configResource()} implemented.
     * 2.  Check it was configured correctly.
     * 3.  Export the full Jenkins configuration via Web UI (commented as it fails with maven defaultProperties).
     * 4.  Export the schema via Web UI (commented out as there is no good validator so far).
     * 5.  Verify the Jenkins configuration against the schema (commented out as there is no good validator so far).
     * 6.  Check the Jenkins configuration is valid via Web UI (used the plugin config so far).
     * 7.  Apply the Jenkins configuration via Web UI (maybe not needed, but we test it as well).
     * 8.  Write the Jenkins configuration to $JENKINS_ROOT/jenkins.yaml.
     * 9.  Restart Jenkins.
     * 10. Check the {@link #stringInLogExpected()} is set during the restart.
     *
     * @throws IOException If an exception is thrown managing resources or files.
     */
    @Test
    public void exportImportRoundTrip() throws IOException {
        String resourcePath = configResource();
        String resourceContent = getResourceContent(resourcePath);

        assertNotNull(resourcePath);
        assertNotNull(resourceContent);

        r.then(step -> {
            // Configure and validate
            configureWithResource(resourcePath);
            assertConfiguredAsExpected(r, resourceContent);

            // Get the full configuration
            String jenkinsConf = getJenkinsConfViaWebUI();

            // Here we check that the exported configuration has the same information as the YAML used.
            // hack: But when JCasC is running under test (not normally) the ciphered secrets in the YAML are ciphered
            // again, so the exported configuration won't be the same never. When running normally it works. When the
            // bug filed is solved we can uncomment that check. In other way, all the plugins with plain secrets in
            // their YAMLs will fail in this test. Example: Credentials.
            //TODO: uncomment when https://issues.jenkins-ci.org/browse/JENKINS-57863 is solved
            assertJenkinsHasConfiguredPath(jenkinsConf, resourcePath);

            //hack: the full Jenkins config fails due to defaultProperties of maven, we use the config of the plugin,
            //but first, we guarantee that the exported configuration is the same as the configured.
            //TODO: remove when https://issues.jenkins-ci.org/browse/JENKINS-57122 is solved
            //@Issue("JENKINS-57122")
            jenkinsConf = getResourceContent(resourcePath);

            // Get the schema
            //String schema = getSchemaViaWebUI();

            // Verify it's compliant to the schema
            // TODO: when YAML schema validation is mature enough we can do that.
            //verifyJsonAgainstSchema(jenkinsConf, schema);

            // Check if the exported configuration is valid
            assertConfigViaWebUI(jenkinsConf);

            // Apply full configuration. Maybe not needed, we already have it configured and checked it is valid.
            applyConfigViaWebUI(jenkinsConf);
            assertConfiguredAsExpected(r, resourceContent);

            // Configure Jenkins default JCasC file with the config file. It's already established, we check if applied
            // looking at the logs.
            putConfigInHome(jenkinsConf);

            // Start recording the logs just before restarting, to avoid capture the previous startup. We're look there
            // if the "magic token" is there
            logging.record("io.jenkins.plugins.casc", Level.INFO).capture(5);
        });

        r.then(step -> {
            // Verify the log shows it's configured
            assertLogAsExpected(stringInLogExpected());

            // Verify the configuration set at home/jenkins.yaml is loaded
            assertConfiguredAsExpected(r, resourceContent);
        });
    }

    private void assertJenkinsHasConfiguredPath(String jenkinsConfig, String resourcePath) throws IOException, URISyntaxException {
        // Get mapping from full Jenkins configuration
        InputStream configStream = IOUtils.toInputStream(jenkinsConfig, Charset.forName("UTF-8"));
        Mapping jenkinsMapping = YamlUtils.loadFrom(Collections.singletonList(YamlSource.of(configStream)));

        // Get mapping from resourcePath
        YamlSource<Path> yamlConfigSource = YamlSource.of(Paths.get(getClass().getResource(resourcePath).toURI()));
        Mapping configMapping = YamlUtils.loadFrom(Collections.singletonList(yamlConfigSource));

        Mapping result = new Mapping();

        // We use a recursive method to compare all nodes of configMapping
        sameInfoAs(jenkinsMapping, configMapping);
    }

    /**
     * Look if the compared node has AT LEAST the same information as values. <code>compared</code> could have more
     * information, but the matching entries must be equals.
     * @param compared a configuration with a lot of information
     * @param values a configuration with less information to compare to
     */
    private void sameInfoAs(CNode compared, CNode values) {
        if (compared.getType() != values.getType()) {
            fail(String.format("The Jenkins configuration exported has an entry of different type of the config entry used.\nJenkins: '%s:%s (%s)'\nConfig file: '%s:%s (%s)'",
                    compared.getSource().file, compared.getSource().line, compared.getType(),
                    values.getSource().file, values.getSource().line, values.getType()));
        }

        switch (values.getType()) {
            case MAPPING:
                // Each entry of values MUST have the same entry on compared
                for (Map.Entry<String, CNode> node : ((Mapping) values).entrySet()) {
                    // We look for the entry of values inside compared and look if they both have the same info
                    for (Map.Entry<String, CNode> node2 : ((Mapping) compared).entrySet()) {
                        if (node2.getKey().equals(node.getKey())) {
                            sameInfoAs(node2.getValue(), node.getValue());
                        }
                    }
                }
                break;

            case SEQUENCE:
                // Each entry of values MUST have an equal entry on compared
                for (CNode node : ((Sequence) values)) {
                    // We look for the entry of values inside compared and look if they both have the same info
                    for (CNode node2 : ((Sequence) compared)) {
                        sameInfoAs(node2, node);
                    }
                }
                break;

            case SCALAR:
                // If both are scalar, just compared then
                Scalar value = (Scalar) values;
                Scalar oneCompared = (Scalar) compared;
                if (!value.getValue().equals(oneCompared.getValue())) {
                    fail(String.format("The Jenkins configuration exported has a value different from the config used. \nJenkins: %s:%s.\nValue: '%s'\nConfig file: %s:%s.\nValue: '%s'",
                            oneCompared.getSource().file, oneCompared.getSource().line, oneCompared.getValue(),
                            value.getSource().file, value.getSource().line, value.getValue()));
                }
        }
    }

    private void configureWithResource(String config) throws ConfiguratorException {
        ConfigurationAsCode.get().configure(this.getClass().getResource(config).toExternalForm());
    }

    private String getResourceContent(String resourcePath) throws IOException {
        return IOUtils.toString(getClass().getResourceAsStream(resourcePath),
                "UTF-8");
    }

    private void writeToFile(String text, String path) throws FileNotFoundException {
        File file = new File(path);
        try(PrintWriter out = new PrintWriter(file)) {
            out.print(text);
        }
    }

    private void putConfigInHome(String config) throws IOException {
        File configFile = new File(r.home, ConfigurationAsCode.DEFAULT_JENKINS_YAML_PATH);

        writeToFile(config, configFile.getAbsolutePath());
        assertTrue(ConfigurationAsCode.DEFAULT_JENKINS_YAML_PATH + " should be created", configFile.exists());
    }

    //TODO: to be used when https://issues.jenkins-ci.org/browse/JENKINS-57122 is solved
    //@Issue("JENKINS-57122")
    private String getJenkinsConfViaWebUI() throws Exception {
        return download("configuration-as-code/export");
    }

    private String download(String url) throws IOException {
        JenkinsRule.WebClient client = r.j.createWebClient();
        WebRequest request = new WebRequest(client.createCrumbedUrl(url), POST);
        WebResponse response = client.loadWebResponse(request);

        assertEquals(200, response.getStatusCode());
        return response.getContentAsString();
    }

    private void assertConfigViaWebUI(String jenkinsConfig) throws Exception {
        // The UI requires the path to the config file
        File f = tempFolder.newFile();
        writeToFile(jenkinsConfig, f.getAbsolutePath());

        // Call the check url
        JenkinsRule.WebClient client = r.j.createWebClient();
        WebRequest request = new WebRequest(client.createCrumbedUrl("configuration-as-code/checkNewSource"), POST);
        NameValuePair param = new NameValuePair("newSource", f.toURI().toURL().toExternalForm());
        request.setRequestParameters(Collections.singletonList(param));
        WebResponse response = client.loadWebResponse(request);
        assertEquals(200, response.getStatusCode());
        String res = response.getContentAsString();
        assertThat(res, containsString("The configuration can be applied"));
    }

    private void applyConfigViaWebUI(String jenkinsConfig) throws Exception {
        // The UI requires the path to the config file
        File f = tempFolder.newFile();
        writeToFile(jenkinsConfig, f.getAbsolutePath());

        // Call the replace url
        JenkinsRule.WebClient client = r.j.createWebClient();
        WebRequest request = new WebRequest(client.createCrumbedUrl("configuration-as-code/replace"), POST);
        NameValuePair param = new NameValuePair("_.newSource", f.toURI().toURL().toExternalForm());
        request.setRequestParameters(Collections.singletonList(param));
        WebResponse response = client.loadWebResponse(request);
        assertEquals(200, response.getStatusCode());
        String res = response.getContentAsString();
        /* The result page has:
        Configuration loaded from :
                        <ul>
                            <li>path</li>
                        </ul>
        path is the file used to store the configuration.
         */
        assertThat(res, containsString(f.toURI().toURL().toExternalForm()));
    }

    private void assertLogAsExpected(String uniqueText) {
        assertTrue("The log should have '" + uniqueText + "'", logging.getMessages().stream().anyMatch(m -> m.contains(uniqueText)));
    }
}
