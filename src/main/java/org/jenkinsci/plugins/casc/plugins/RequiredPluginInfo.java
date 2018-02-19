package org.jenkinsci.plugins.casc.plugins;

import hudson.util.VersionNumber;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Restricted(NoExternalUse.class)
public class RequiredPluginInfo {

    private static final Logger LOGGER = Logger.getLogger(RequiredPluginInfo.class.getName());
    private static final Pattern QUANTIFIER = Pattern.compile("(<=|>=)(.*)");
    private String pluginId;
    private String version;
    private VersionRange range;

    public enum VersionRange {
       NO_RANGE(""),
       GREATER_OR_EQUAL(">="),
       LESS_OR_EQUAL("<=");

       private String range;

       VersionRange(String range) {
           this.range = range;
       }

       static VersionRange fromString(String value) {
           for(VersionRange v : VersionRange.values()) {
               if(v.range.equals(value)) {
                   return v;
               }
           }
           return NO_RANGE;
       }
    }

    @DataBoundConstructor
    public RequiredPluginInfo(String pluginId, String version) {
        Matcher m = QUANTIFIER.matcher(version);
        this.version = m.matches() ? m.group(2).trim() : version;
        this.pluginId = pluginId;
        this.range = m.matches() ? VersionRange.fromString(m.group(1)) : VersionRange.NO_RANGE;
    }

    public VersionRange getRange() {
        return this.range;
    }

    public String getPluginId() {
        return pluginId;
    }

    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

    public String getVersion() {
        return version;
    }

    public VersionNumber toVersionNumberObject() {
        return new VersionNumber(getVersion());
    }

    //Compares to an installed plugin
    public boolean needsInstall(VersionNumber vn) {
        VersionNumber currentVersion = toVersionNumberObject();
        if(this.getRange().equals(VersionRange.GREATER_OR_EQUAL)) {
            return vn.isOlderThan(currentVersion);
        } else if (this.getRange().equals(VersionRange.LESS_OR_EQUAL)) {
            if(vn.isNewerThan(currentVersion)) {
                LOGGER.fine(String.format("Installed plugin '%s' is too new for this configuration.", pluginId));
                return false;
            } else {
                return true;
            }
        } else {
            //They must be the same version
            return vn.equals(toVersionNumberObject());
        }
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
