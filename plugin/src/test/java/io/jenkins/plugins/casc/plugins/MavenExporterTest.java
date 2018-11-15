package io.jenkins.plugins.casc.plugins;

import hudson.PluginWrapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
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
        Assert.assertEquals("Jenkins user build vars plugin", actual.getLongName());
        Assert.assertEquals("Sets username build variables", actual.getDescription());
        Assert.assertEquals("http://wiki.jenkins-ci.org/display/JENKINS/Build+User+Vars+Plugin", actual.getUrl());
        Assert.assertEquals("1.609.1", actual.getJenkinsVersion());
        Assert.assertEquals("mailer:1.16", actual.getPluginDependencies());
    }


}
