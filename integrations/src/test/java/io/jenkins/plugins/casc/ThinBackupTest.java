package io.jenkins.plugins.casc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.plugins.thinbackup.ThinBackupPluginImpl;

public class ThinBackupTest {

    @Rule
    public JenkinsConfiguredWithReadmeRule j = new JenkinsConfiguredWithReadmeRule();

    @Test
    @ConfiguredWithReadme("thin-backup/README.md")
    public void configure_thinbackup() {
        ThinBackupPluginImpl thinBackupPluginConfig = ThinBackupPluginImpl.get();
        final String backupPath = thinBackupPluginConfig.getBackupPath();
        // test strings
        assertEquals("c:\\temp\\thin-backup", backupPath);
        assertEquals("0 12 * * 1-5", thinBackupPluginConfig.getDiffBackupSchedule());
        assertEquals("0 12 * * 1", thinBackupPluginConfig.getFullBackupSchedule());
        assertEquals("^.*\\.(log)$", thinBackupPluginConfig.getExcludedFilesRegex());
        assertEquals("^.*\\.(txt)$", thinBackupPluginConfig.getBackupAdditionalFilesRegex());
        // test numbers
        assertEquals(120, thinBackupPluginConfig.getForceQuietModeTimeout());
        assertEquals(-1, thinBackupPluginConfig.getNrMaxStoredFull());
        // test booleans
        assertTrue(thinBackupPluginConfig.isWaitForIdle());
        assertTrue(thinBackupPluginConfig.isBackupBuildResults());
        assertTrue(thinBackupPluginConfig.isFailFast());

        assertFalse(thinBackupPluginConfig.isCleanupDiff());
        assertFalse(thinBackupPluginConfig.isMoveOldBackupsToZipFile());
        assertFalse(thinBackupPluginConfig.isBackupBuildArchive());
        assertFalse(thinBackupPluginConfig.isBackupPluginArchives());
        assertFalse(thinBackupPluginConfig.isBackupUserContents());
        assertFalse(thinBackupPluginConfig.isBackupConfigHistory());
        assertFalse(thinBackupPluginConfig.isBackupAdditionalFiles());
        assertFalse(thinBackupPluginConfig.isBackupNextBuildNumber());
        assertFalse(thinBackupPluginConfig.isBackupBuildsToKeepOnly());
    }
}
