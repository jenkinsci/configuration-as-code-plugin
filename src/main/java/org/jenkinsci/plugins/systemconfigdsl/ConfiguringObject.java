package org.jenkinsci.plugins.systemconfigdsl;

import groovy.lang.GroovyObjectSupport;

/**
 * @author Kohsuke Kawaguchi
 */
public class ConfiguringObject extends GroovyObjectSupport {
    /**
     * Run the given closure with 'this' as delegate
     */
    /*package*/ void runWith(ConfigScript s) throws Exception {
        s.setDelegate(this);
        s.run();
    }
}
