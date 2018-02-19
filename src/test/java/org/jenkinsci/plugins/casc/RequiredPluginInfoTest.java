package org.jenkinsci.plugins.casc;

import hudson.util.VersionNumber;
import org.jenkinsci.plugins.casc.plugins.RequiredPluginInfo;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RequiredPluginInfoTest {

    @Test
    public void testGreaterAndLesserThan() {
        RequiredPluginInfo rinfo = new RequiredPluginInfo("git", "<= 3.6.0");
        assertEquals(RequiredPluginInfo.VersionRange.LESS_OR_EQUAL, rinfo.getRange());
        RequiredPluginInfo rinfo2 = new RequiredPluginInfo("git", ">= 3.6.0");
        assertEquals(RequiredPluginInfo.VersionRange.GREATER_OR_EQUAL, rinfo2.getRange());
    }

    @Test
    public void testNoRangeSpecified() {
        RequiredPluginInfo rinfo = new RequiredPluginInfo("git", "3.6.0");
        assertEquals(RequiredPluginInfo.VersionRange.NO_RANGE, rinfo.getRange());
    }

    @Test
    public void testUpToDateSpecification() {
        RequiredPluginInfo rinfo = new RequiredPluginInfo("git", "<= 3.6.0");
        VersionNumber vn = new VersionNumber("3.5.0");
        assertTrue(rinfo.needsInstall(vn));
        VersionNumber vn2 = new VersionNumber("3.7.0");
        assertFalse(rinfo.needsInstall(vn2));
    }
}
