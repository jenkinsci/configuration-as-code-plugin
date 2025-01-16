package io.jenkins.plugins.casc.misc.junit.jupiter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.htmlunit.HttpMethod.POST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jenkins.plugins.casc.ConfigurationAsCode;
import io.jenkins.plugins.casc.ConfiguratorException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import org.apache.commons.io.IOUtils;
import org.htmlunit.WebRequest;
import org.htmlunit.WebResponse;
import org.htmlunit.util.NameValuePair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Base test to check a complete test of each plugin configuration.
 * <p>
 * What it does:
 * <ol>
 *  <li>Configure the instance with the {@link #configResource()} implemented.</li>
 *  <li>Check it was configured correctly.</li>
 *  <li>Check the configuration is valid via Web UI.</li>
 *  <li>Apply the configuration via Web UI.</li>
 *  <li>Write the configuration to $JENKINS_HOME/jenkins.yaml.</li>
 *  <li>Restart Jenkins.</li>
 *  <li>Check the {@link #stringInLogExpected()} is set during the restart.</li>
 *  <li>Check it is still configured correctly after the restart</li>
 * </ol>
 * <p>
 * All the plugin author needs to do is override the methods providing:
 * <ol>
 *  <li>The resource with the yaml configuration of the plugin in case they use their own name for the file.</li>
 *  <li>A way to validate the configuration is established.</li>
 *  <li>A string that should be present in the logs (casc logger) that guarantees the config is loaded. Usually a weird text configured.</li>
 * </ol>
 * <p>
 * This is the JUnit5 equivalent of {@link io.jenkins.plugins.casc.misc.RoundTripAbstractTest}
 *
 * @see io.jenkins.plugins.casc.misc.RoundTripAbstractTest
 */
@WithJenkins
public abstract class AbstractRoundTripTest {

    @TempDir
    public Path tempFolder;

    /**
     * A method to assert if the configuration was correctly loaded. The Jenkins rule and the
     * content of the config supposedly loaded are passed.
     *
     * @param j a JenkinsRule instance.
     * @param configContent expected configuration.
     */
    protected abstract void assertConfiguredAsExpected(JenkinsRule j, String configContent);

    /**
     * Return the resource path (yaml file) to be loaded. i.e: If the resource is in the same
     * package of the implementor class, then: my-config.yaml
     *
     * @return the resource name and path.
     */
    protected String configResource() {
        return "configuration-as-code.yaml";
    }

    /**
     * Return the string that should be in the logs of the JCasC logger to verify it's configured
     * after a restart. This string should be unique to avoid interpreting that it was configured
     * successfully, but it wasn't.
     *
     * @return the unique string to be in the logs to certify the configuration was done
     * successfully.
     */
    protected abstract String stringInLogExpected();

    /**
     * 1. Configure the instance with the {@link #configResource()} implemented. 2. Check it was
     * configured correctly. 3. Check the configuration is valid via Web UI. 4. Apply the
     * configuration via Web UI. 5. Write the configuration to $JENKINS_HOME/jenkins.yaml. 6.
     * Restart Jenkins. 7. Check the {@link #stringInLogExpected()} is set during the restart.
     *
     * @throws IOException If an exception is thrown managing resources or files.
     */
    @Test
    public void roundTripTest(JenkinsRule r) throws Throwable {
        String resourcePath = configResource();
        String resourceContent = getResourceContent(resourcePath);

        assertNotNull(resourcePath);
        assertNotNull(resourceContent);

        // Configure and validate
        configureWithResource(resourcePath);
        assertConfiguredAsExpected(r, resourceContent);

        // Check config is valid via Web UI
        String jenkinsConf = getResourceContent(resourcePath);
        assertConfigViaWebUI(r, jenkinsConf);

        // Apply configuration via Web UI
        applyConfigViaWebUI(r, jenkinsConf);
        assertConfiguredAsExpected(r, resourceContent);

        // Configure Jenkins default JCasC file with the config file. It's already established, we check if applied
        // looking at the logs.
        putConfigInHome(r, jenkinsConf);

        // Start recording the logs just before restarting, to avoid capture the previous startup. We're look there
        // if the "magic token" is there
        List<String> messages = new ArrayList<>();
        Logger logger = Logger.getLogger("io.jenkins.plugins.casc");
        logger.addHandler(new ConsoleHandler() {
            @Override
            public void publish(LogRecord record) {
                super.publish(record);

                String message = new SimpleFormatter().formatMessage(record);
                Throwable x = record.getThrown();
                synchronized (messages) {
                    messages.add(message == null && x != null ? x.toString() : message);
                }
            }
        });
        logger.setLevel(Level.ALL);

        // Restart the testing instance
        r.restart();

        // Verify the log shows it's configured
        assertLogAsExpected(messages, stringInLogExpected());

        // Verify the configuration set at home/jenkins.yaml is loaded
        assertConfiguredAsExpected(r, resourceContent);
    }

    private void configureWithResource(String config) throws ConfiguratorException {
        ConfigurationAsCode.get().configure(this.getClass().getResource(config).toExternalForm());
    }

    private String getResourceContent(String resourcePath) throws IOException {
        return IOUtils.toString(getClass().getResourceAsStream(resourcePath), StandardCharsets.UTF_8);
    }

    private void writeToFile(String text, String path) throws FileNotFoundException {
        File file = new File(path);
        try (PrintWriter out = new PrintWriter(file)) {
            out.print(text);
        }
    }

    private void putConfigInHome(JenkinsRule r, String config) throws Exception {
        File configFile = new File(r.getWebAppRoot(), ConfigurationAsCode.DEFAULT_JENKINS_YAML_PATH);

        writeToFile(config, configFile.getAbsolutePath());
        assertTrue(configFile.exists(), ConfigurationAsCode.DEFAULT_JENKINS_YAML_PATH + " should be created");
    }

    private void assertConfigViaWebUI(JenkinsRule r, String jenkinsConfig) throws Exception {
        // The UI requires the path to the config file
        File f = File.createTempFile("junit", null, tempFolder.toFile());
        writeToFile(jenkinsConfig, f.getAbsolutePath());

        // Call the check url
        JenkinsRule.WebClient client = r.createWebClient();
        WebRequest request = new WebRequest(client.createCrumbedUrl("configuration-as-code/checkNewSource"), POST);
        NameValuePair param = new NameValuePair("newSource", f.toURI().toURL().toExternalForm());
        request.setRequestParameters(Collections.singletonList(param));
        WebResponse response = client.loadWebResponse(request);
        assertEquals(
                200,
                response.getStatusCode(),
                "Failed to POST to " + request.getUrl().toString());
        String res = response.getContentAsString();
        assertThat(res, containsString("The configuration can be applied"));
    }

    private void applyConfigViaWebUI(JenkinsRule r, String jenkinsConfig) throws Exception {
        // The UI requires the path to the config file
        File f = File.createTempFile("junit", null, tempFolder.toFile());
        writeToFile(jenkinsConfig, f.getAbsolutePath());

        // Call the replace url
        JenkinsRule.WebClient client = r.createWebClient();
        WebRequest request = new WebRequest(client.createCrumbedUrl("configuration-as-code/replace"), POST);
        NameValuePair param = new NameValuePair("_.newSource", f.toURI().toURL().toExternalForm());
        request.setRequestParameters(Collections.singletonList(param));
        request.setRequestParameters(Collections.singletonList(param));
        WebResponse response = client.loadWebResponse(request);
        assertEquals(
                200,
                response.getStatusCode(),
                "Failed to POST to " + request.getUrl().toString());
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

    private void assertLogAsExpected(List<String> messages, String uniqueText) {
        assertTrue(messages.stream().anyMatch(m -> m.contains(uniqueText)), "The log should have '" + uniqueText + "'");
    }
}
