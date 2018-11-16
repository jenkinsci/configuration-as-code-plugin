package io.jenkins.plugins.casc.plugins;

import org.apache.maven.model.Dependency;

/**
 * A superset of {@link Dependency} such that we can add comments to aid traceability.
 */
public class ExtendedDependency extends Dependency {

    private String extendedVersion;
    private String longName;
    private String description;
    private String url;
    private String jenkinsVersion;
    private String pluginDependencies;

    public String getExtendedVersion() {
        return extendedVersion;
    }

    public void setExtendedVersion(final String extendedVersion) {
        this.extendedVersion = extendedVersion;
    }

    public String getLongName() {
        return longName;
    }

    public void setLongName(final String longName) {
        this.longName = longName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public String getJenkinsVersion() {
        return jenkinsVersion;
    }

    public void setJenkinsVersion(final String jenkinsVersion) {
        this.jenkinsVersion = jenkinsVersion;
    }

    public String getPluginDependencies() {
        return pluginDependencies;
    }

    public void setPluginDependencies(final String pluginDependencies) {
        this.pluginDependencies = pluginDependencies;
    }

}
