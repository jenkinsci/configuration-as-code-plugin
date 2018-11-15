package io.jenkins.plugins.casc.plugins;

import hudson.PluginWrapper;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import java.util.jar.Attributes;
import java.util.jar.Manifest;

@Restricted(NoExternalUse.class)
public class MavenExporter {

    static ExtendedDependency exportPlugin(final PluginWrapper pluginWrapper) {
        final ExtendedDependency dep = new ExtendedDependency();
        final Manifest manifest = pluginWrapper.getManifest();
        final Attributes attributes = manifest.getMainAttributes();

        dep.setGroupId(attributes.getValue("Group-Id"));
        dep.setArtifactId(pluginWrapper.getShortName());
        dep.setVersion(pluginWrapper.getVersion());

        dep.setLongName(attributes.getValue("Long-Name"));
        dep.setDescription(attributes.getValue("Specification-Title"));
        dep.setUrl(attributes.getValue("Url"));
        dep.setJenkinsVersion(attributes.getValue("Jenkins-Version"));
        dep.setPluginDependencies(attributes.getValue("Plugin-Dependencies"));

        return dep;
    }



}
