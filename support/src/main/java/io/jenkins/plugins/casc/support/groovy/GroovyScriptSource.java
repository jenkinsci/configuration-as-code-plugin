package io.jenkins.plugins.casc.support.groovy;

import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;

import java.io.IOException;

/**
 * @author <a href="mailto:tomasz.szandala@gmail.com">Tomasz Szandala</a>
 */
public abstract class GroovyScriptSource extends AbstractDescribableImpl<GroovyScriptSource> implements ExtensionPoint {

    public abstract String getScript() throws IOException;
}
