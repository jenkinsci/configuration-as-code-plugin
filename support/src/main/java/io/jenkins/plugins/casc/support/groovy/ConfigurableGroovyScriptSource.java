package io.jenkins.plugins.casc.support.groovy;

import io.jenkins.plugins.casc.Configurable;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.model.CNode;

/**
 * @author <a href="mailto:tomasz.szandala@gmail.com">Tomasz Szandala</a>
 */
public abstract class ConfigurableGroovyScriptSource extends GroovyScriptSource implements Configurable {

    @Override
    public void configure(CNode node) throws ConfiguratorException {
        configure(node.asScalar().getValue());
    }

    protected abstract void configure(String value);

    @Override
    public void check(CNode node) throws ConfiguratorException {
        node.asScalar();
    }

    @Override
    public CNode describe() throws Exception {
        return null; // Not relevant here
    }

}
