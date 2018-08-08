package io.jenkins.plugins.casc.support.jobdsl;

import hudson.Extension;
import hudson.model.Descriptor;
import io.jenkins.plugins.casc.Configurable;
import org.jenkinsci.Symbol;

import java.io.IOException;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class InlineGroovyScriptSource extends ConfigurableScriptSource implements Configurable {

    public String script;

    @Override
    public void configure(String script) {
        this.script = script;
    }

    @Override
    public String getScript() throws IOException {
        return script;
    }

    @Extension
    @Symbol("script")
    public static class DescriptorImpl extends Descriptor<ScriptSource> {

    }
}
