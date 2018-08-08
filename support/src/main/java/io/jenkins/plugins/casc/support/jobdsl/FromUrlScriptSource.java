package io.jenkins.plugins.casc.support.jobdsl;

import hudson.Extension;
import hudson.model.Descriptor;
import io.jenkins.plugins.casc.Configurable;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.Symbol;

import java.io.IOException;
import java.net.URI;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class FromUrlScriptSource extends ConfigurableScriptSource implements Configurable {

    public String url;

    @Override
    public void configure(String url) {
        this.url = url;
    }

    @Override
    public String getScript() throws IOException {
        return IOUtils.toString(URI.create(url));
    }

    @Extension
    @Symbol("url")
    public static class DescriptorImpl extends Descriptor<ScriptSource> {

    }
}
