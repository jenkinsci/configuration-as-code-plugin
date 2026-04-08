package io.jenkins.plugins.casc.history;

import hudson.ExtensionPoint;
import java.io.IOException;

public abstract class CasCHistoryBackend implements ExtensionPoint {

    public abstract void save(String yamlContent, String triggeredBy) throws IOException;
}
