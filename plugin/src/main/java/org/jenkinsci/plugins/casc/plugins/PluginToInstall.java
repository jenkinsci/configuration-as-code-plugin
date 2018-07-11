package org.jenkinsci.plugins.casc.plugins;

import hudson.model.UpdateCenter;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import java.util.Objects;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Restricted(NoExternalUse.class)
class PluginToInstall {
    public final String site;
    public final String shortname;
    public final String version;

    public PluginToInstall(String shortname, String version) {
        this.shortname = shortname;
        final int i = version.indexOf('@');
        this.version = i < 0 ? version : version.substring(0,i);
        this.site = i < 0 ? UpdateCenter.PREDEFINED_UPDATE_SITE_ID : version.substring(i+1);
    }

    @Override
    public String toString() {
        return new StringBuilder(shortname)
                .append(':').append(version)
                .append('@').append(site).toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PluginToInstall that = (PluginToInstall) o;
        return Objects.equals(site, that.site) &&
                Objects.equals(shortname, that.shortname) &&
                Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {

        return Objects.hash(site, shortname, version);
    }
}
