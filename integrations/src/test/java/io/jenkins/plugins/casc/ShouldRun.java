package io.jenkins.plugins.casc;

import java.io.File;

/* intentionally package protected so that not accessible outside this package */
class ShouldRun {
    private static boolean isWindows() {
        return File.pathSeparatorChar == ';';
    }

    private static int testCounter = 0;
    private static int buildNumber = -1;

    /** Return true if this test should be executed
     * @return true if this test should be executed
     */
    public static boolean thisTest() {
        if (!isWindows()) {
            return true; // Tests are fast enough on Linux, run them all
        }
        if (buildNumber < 0) {
            String buildNumberStr = System.getenv("BUILD_NUMBER");
            if (buildNumberStr == null) {
                buildNumberStr = "1";
            }
            buildNumber = Integer.parseInt(buildNumberStr);
        }
        testCounter++;
        // Run 80% of tests
        // Use buildNumber as an offset so that ci.jenkins.io cycles through all tests
        return (buildNumber + testCounter) % 5 < 4;
    }
}
