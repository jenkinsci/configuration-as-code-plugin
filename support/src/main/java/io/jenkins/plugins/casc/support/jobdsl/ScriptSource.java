package io.jenkins.plugins.casc.support.jobdsl;

import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;

import java.io.IOException;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public abstract class ScriptSource extends AbstractDescribableImpl<ScriptSource> implements ExtensionPoint {

    public abstract String getScript() throws IOException;
}
