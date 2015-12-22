package org.jenkinsci.plugins.systemconfigdsl;

import groovy.lang.Closure;
import groovy.lang.GString;
import groovy.lang.GroovyObjectSupport;
import hudson.model.Descriptor;
import org.jenkinsci.plugins.symbol.SymbolLookup;

/**
 * Support for creating a builder pattern.
 *
 * @author Kohsuke Kawaguchi
 */
abstract class BuilderSupport extends GroovyObjectSupport {

    /**
     * Process arguments.
     */
    protected Object[] massageArgs(Object args) {
        Object[] r;
        if (args instanceof Object[])
            r = (Object[])args;
        else
            r = new Object[]{args};

        // eval GStrings
        for (int i=0; i<r.length; i++) {
            if (r[i] instanceof GString)
                r[i] = r[i].toString();
        }
        return r;
    }

    /**
     * Does the argument array match the list of expected types.
     */
    protected boolean matches(Object[] args, Class... expectedTypes) {
        if (args.length!=expectedTypes.length)
            return false;

        for (int i=0; i<args.length; i++) {
            if (args[i]!=null && !expectedTypes[i].isInstance(args[i]))
                return false;
        }

        return true;
    }

    protected Class lookup(String symbol) {
        Descriptor d = SymbolLookup.get().find(Descriptor.class,symbol);
        if (d==null)    return null;
        return d.clazz;
    }

    /**
     * Builds a value from a closure.
     */
    protected Object buildValue(String symbol, Closure body) {
        Class describable = lookup(symbol);
        if (describable!=null) {
            DescribableFactory f = new DescribableFactory(describable);
            if (body!=null) {
                body.setDelegate(f);
                body.setResolveStrategy(Closure.DELEGATE_FIRST);
                body.call();
            }
            return f.instantiate();
        }
        return null;
    }
}
