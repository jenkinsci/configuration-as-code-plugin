package org.jenkinsci.plugins.casc.support.jobdsl;

import org.jenkinsci.plugins.casc.Configurable;
import org.jenkinsci.plugins.casc.ConfiguratorException;
import org.jenkinsci.plugins.casc.model.CNode;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public abstract class ConfigurableScriptSource extends ScriptSource implements Configurable {

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
