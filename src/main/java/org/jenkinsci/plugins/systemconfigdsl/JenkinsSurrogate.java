package org.jenkinsci.plugins.systemconfigdsl;

import jenkins.model.Jenkins;

/**
 * Root closure delegate that supports pseudo global functions
 * as well as configuring the singleton {@link Jenkins} instance.
 *
 * @author Kohsuke Kawaguchi
 */
public class JenkinsSurrogate extends Surrogate {
    private final Jenkins jenkins;

    public JenkinsSurrogate(Jenkins target) {
        super(target);
        this.jenkins = target;
    }
}
