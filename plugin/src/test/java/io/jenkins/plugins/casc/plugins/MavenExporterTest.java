package io.jenkins.plugins.casc.plugins;

import hudson.PluginWrapper;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.jar.Manifest;

/**
 * A class to test {@link MavenExporter}.
 */
public class MavenExporterTest {

    private PluginWrapper createPluginWrapper(final String baseName) throws IOException {
        final File archive = new File(baseName + ".jpi");
        final Manifest manifest = new Manifest();
        try(final InputStream is = MavenExporter.openResourceStream(this.getClass(), baseName + ".mf")) {
            manifest.read(is);
        }
        final File disableFile = new File(archive.getName() + ".disabled");
        return new PluginWrapper(null, archive, manifest, null, null, disableFile, Collections.emptyList(), Collections.emptyList());
    }

    @Test public void exportPlugin_buildUserVarsPlugin() throws IOException {
        final PluginWrapper pw = createPluginWrapper("build-user-vars-plugin");

        final ExtendedDependency actual = MavenExporter.exportPlugin(pw);

        Assert.assertNotNull(actual);
        Assert.assertEquals("org.jenkins-ci.plugins", actual.getGroupId());
        Assert.assertEquals("build-user-vars-plugin", actual.getArtifactId());
        Assert.assertEquals("1.5", actual.getVersion());
        Assert.assertEquals("1.5", actual.getExtendedVersion());
        Assert.assertEquals("Jenkins user build vars plugin", actual.getLongName());
        Assert.assertEquals("Sets username build variables", actual.getDescription());
        Assert.assertEquals("http://wiki.jenkins-ci.org/display/JENKINS/Build+User+Vars+Plugin", actual.getUrl());
        Assert.assertEquals("1.609.1", actual.getJenkinsVersion());
        Assert.assertEquals("mailer:1.16", actual.getPluginDependencies());
    }

    @Test public void exportPlugin_configurationAsCodePlugin() throws IOException {
        final PluginWrapper pw = createPluginWrapper("configuration-as-code-plugin");

        final ExtendedDependency actual = MavenExporter.exportPlugin(pw);

        Assert.assertNotNull(actual);
        Assert.assertEquals("io.jenkins", actual.getGroupId());
        Assert.assertEquals("configuration-as-code", actual.getArtifactId());
        Assert.assertEquals("1.4-SNAPSHOT", actual.getVersion());
        Assert.assertEquals("1.4-SNAPSHOT (private-5b67d196-odagenais)", actual.getExtendedVersion());
        Assert.assertEquals("Configuration as Code Plugin", actual.getLongName());
        Assert.assertEquals("Manage Jenkins master configuration as code", actual.getDescription());
        Assert.assertEquals("https://wiki.jenkins-ci.org/display/JENKINS/Configuration+as+Code+Plugin", actual.getUrl());
        Assert.assertEquals("2.60.3", actual.getJenkinsVersion());
        Assert.assertEquals("", actual.getPluginDependencies());
    }

    @Test public void exportPlugin_gradlePlugin() throws IOException {
        final PluginWrapper pw = createPluginWrapper("gradle");

        final ExtendedDependency actual = MavenExporter.exportPlugin(pw);

        Assert.assertNotNull(actual);
        Assert.assertEquals("org.jenkins-ci.plugins", actual.getGroupId());
        Assert.assertEquals("gradle", actual.getArtifactId());
        Assert.assertEquals("1.29", actual.getVersion());
        Assert.assertEquals("1.29", actual.getExtendedVersion());
        Assert.assertEquals("Gradle Plugin", actual.getLongName());
        Assert.assertEquals("", actual.getDescription());
        Assert.assertEquals("http://wiki.jenkins-ci.org/display/JENKINS/Gradle+Plugin", actual.getUrl());
        Assert.assertEquals("1.642.1", actual.getJenkinsVersion());
        Assert.assertEquals("structs:1.3", actual.getPluginDependencies());
    }

    @Test public void exportPlugins_simplestPom_one()
            throws IOException, SAXException, TransformerException, XPathExpressionException {
        final PluginWrapper[] plugins = {
            createPluginWrapper("display-url-api"),
        };
        final List<PluginWrapper> pluginList = Arrays.asList(plugins);
        final Document actual;
        try(
            final InputStream is = MavenExporter.openResourceStream(this.getClass(), "simplestPom-input.xml");
            final Reader reader = new InputStreamReader(is);
            ) {

            actual = MavenExporter.exportPlugins(pluginList, reader);
        }

        Assert.assertNotNull(actual);
        assertXmlEquals("simplestPom_one-expected.xml", actual);
    }

    @Test public void exportPlugins_simplestPom_casc()
            throws IOException, SAXException, TransformerException, XPathExpressionException {
        final PluginWrapper[] plugins = {
            createPluginWrapper("configuration-as-code-plugin"),
        };
        final List<PluginWrapper> pluginList = Arrays.asList(plugins);
        final Document actual;
        try(
            final InputStream is = MavenExporter.openResourceStream(this.getClass(), "simplestPom-input.xml");
            final Reader reader = new InputStreamReader(is);
            ) {

            actual = MavenExporter.exportPlugins(pluginList, reader);
        }

        Assert.assertNotNull(actual);
        assertXmlEquals("simplestPom_casc-expected.xml", actual);
    }

    @Test public void exportPlugins_defaultPom_three()
            throws IOException, SAXException, TransformerException, XPathExpressionException {
        final PluginWrapper[] plugins = {
            createPluginWrapper("build-user-vars-plugin"),
            createPluginWrapper("display-url-api"),
            createPluginWrapper("mailer"),
        };
        final List<PluginWrapper> pluginList = Arrays.asList(plugins);

        final Document actual = MavenExporter.exportPlugins(pluginList);

        Assert.assertNotNull(actual);
        assertXmlEquals("defaultPom_three-expected.xml", actual);
    }

    private static void assertXmlEquals(final String expectedFilename, final Document actualDoc)
            throws TransformerException, IOException {
        final String actualXml = toXmlString(actualDoc);
        try(
                final InputStream exIs = MavenExporter.openResourceStream(MavenExporterTest.class, expectedFilename);
                final Reader exReader = new InputStreamReader(exIs);
                final BufferedReader exBr = new BufferedReader(exReader);
                final Reader acReader = new StringReader(actualXml);
                final BufferedReader acBr = new BufferedReader(acReader);
                ) {
            int lineNumber = 1;
            String expectedLine, actualLine;
            while ((expectedLine = exBr.readLine()) != null) {
                actualLine = acBr.readLine();
                final String template = "Difference found at line #%d";
                final String message = String.format(template, lineNumber);
                Assert.assertEquals(message, expectedLine, actualLine);
                lineNumber++;
            }
            actualLine = acBr.readLine();
            final String template = "Difference found at line #%d";
            final String message = String.format(template, lineNumber);
            Assert.assertEquals(message, null, actualLine);
        }

    }

    private static String toXmlString(final Document document)
            throws TransformerException {
        final StringWriter sw = new StringWriter();
        MavenExporter.writeDocument(document, sw);
        return sw.toString();
    }

}
