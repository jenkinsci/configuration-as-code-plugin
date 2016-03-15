package org.jenkinsci.plugins.systemconfigdsl;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.Script;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class BaseClass extends Script {
    public BaseClass() {
    }

    public BaseClass(Binding binding) {
        super(binding);
    }

    public Nested foo(String name) {
        return new Nested();
    }

    public static class Nested {
        public void call(Closure c) {
            System.out.println("Yo!");
        }
    }
}
