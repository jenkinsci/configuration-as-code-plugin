package org.jenkinsci.plugins.systemconfigdsl;

import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.codehaus.groovy.control.CompilerConfiguration;

import java.io.InputStreamReader;

/**
 * @author Kohsuke Kawaguchi
 */
public class Foo {
    public static void main(String[] args) {
        CompilerConfiguration cc = new CompilerConfiguration();
        cc.setScriptBaseClass(BaseClass.class.getName());
        GroovyShell sh = new GroovyShell(cc);
        Script s = sh.parse(new InputStreamReader(Foo.class.getResourceAsStream("/foo.conf")));
        s.run();
    }
}
