package org.jenkinsci.plugins.systemconfigdsl;

import groovy.lang.Binding;
import groovy.lang.GroovyObject;
import groovy.lang.Script;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class ConfigScript extends Script {
    private GroovyObject delegate;

    protected ConfigScript() {
        super();
    }

    protected ConfigScript(Binding binding) {
        super(binding);
    }

    /**
     * Sets the delegation target.
     */
    public void setDelegate(GroovyObject delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object invokeMethod(String name, Object args) {
        return delegate.invokeMethod(name,args);
    }

    @Override
    public Object getProperty(String property) {
        return delegate.getProperty(property);
    }

    @Override
    public void setProperty(String property, Object newValue) {
        delegate.setProperty(property,newValue);
        // don't allow Binding to be looked up as that'd allow any assignments,
        // thereby hiding invalid configuration such a typo.
    }
}