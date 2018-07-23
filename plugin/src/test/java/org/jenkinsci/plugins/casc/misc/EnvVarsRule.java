package org.jenkinsci.plugins.casc.misc;

import org.junit.contrib.java.lang.system.EnvironmentVariables;

/**
 * @author lanwen (Kirill Merkushev)
 */
public class EnvVarsRule extends EnvironmentVariables {
    public EnvVarsRule env(String name, String value) {
        set(name, value);
        return this;
    }
}
