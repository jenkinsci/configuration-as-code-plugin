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

    @Test public void exportPlugin_buildUserVarsPlugin() throws IOException {
        final File archive = new File("build-user-vars-plugin.jpi");
        final Manifest manifest = new Manifest();
        try(final InputStream is = MavenExporter.openResourceStream(this.getClass(), "build-user-vars-plugin.mf")) {
            manifest.read(is);
        }
        final File disableFile = new File(archive.getName() + ".disabled");
        final PluginWrapper pw = new PluginWrapper(null, archive, manifest, null, null, disableFile, Collections.emptyList(), Collections.emptyList());

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
