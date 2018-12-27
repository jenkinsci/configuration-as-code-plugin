package io.jenkins.plugins.casc.support.groovy;

import hudson.Extension;
import hudson.model.Descriptor;
import org.apache.commons.io.FileUtils;
import org.jenkinsci.Symbol;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author <a href="mailto:tomasz.szandala@gmail.com">Tomasz Szandala</a>
 */
public class FromFileGroovyScriptSource extends ConfigurableGroovyScriptSource {

    public String path;

    @Override
    public void configure(String path) {
        this.path = path;
    }

    @Override
    public String getScript() throws IOException {
        return FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8);
    }

    @Extension
    @Symbol("file")
    public static class DescriptorImpl extends Descriptor<GroovyScriptSource> {

    }
}
