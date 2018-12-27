package io.jenkins.plugins.casc.support.groovy;

import hudson.Extension;
import hudson.model.Descriptor;
import io.jenkins.plugins.casc.Configurable;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.Symbol;

import java.io.IOException;
import java.net.URI;

/**
 * @author <a href="mailto:tomasz.szandala@gmail.com">Tomasz Szandala</a>
 */
public class FromUrlGroovyScriptSource extends ConfigurableGroovyScriptSource implements Configurable {

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
    public static class DescriptorImpl extends Descriptor<GroovyScriptSource> {

    }
}
