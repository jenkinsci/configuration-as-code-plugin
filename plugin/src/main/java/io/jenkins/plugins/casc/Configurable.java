package io.jenkins.plugins.casc;

import io.jenkins.plugins.casc.model.CNode;

/**
 * API for components to directly implement Configuration-as-Code.
 * A default constructor is required.
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public interface Configurable {

    void configure(CNode node) throws ConfiguratorException;

    void check(CNode node) throws ConfiguratorException;

    CNode describe() throws Exception;

}
