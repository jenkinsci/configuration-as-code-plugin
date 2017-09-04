package org.jenkinsci.plugins.casc;

import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import org.apache.commons.beanutils.PropertyUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.accmod.Restricted;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension
public class JenkinsConfigurator extends BaseConfigurator<Jenkins> implements RootElementConfigurator {

    @Override
    public Class<Jenkins> getTarget() {
        return Jenkins.class;
    }

    @Override
    public Jenkins configure(Object c) throws Exception {
        Map config = (Map) c;
        Jenkins jenkins = Jenkins.getInstance();

        final Set<Attribute> attributes = describe();

        for (Attribute attribute : attributes) {
            final String name = attribute.getName();
            if (config.containsKey(name)) {
                final Object sub = config.get(name);
                if (attribute.isMultiple()) {
                    List values = new ArrayList<>();
                    for (Object o : (List) sub) {
                        Object value = Configurator.lookup(attribute.getType()).configure(o);
                        values.add(value);
                    }
                    attribute.setValue(jenkins, values);
                } else {
                    Object value = Configurator.lookup(attribute.getType()).configure(sub);
                    attribute.setValue(jenkins, value);
                }
            }
        }
        return jenkins;
    }


    @Override
    public Set<Attribute> describe() {
        final Set<Attribute> attributes = super.describe();

        final List<ExtensionPoint> all = Jenkins.getInstance().getExtensionList(ExtensionPoint.class);
        for (Object e : all) {
            if (e instanceof Descriptor) continue;
            final Symbol symbol = e.getClass().getAnnotation(Symbol.class);
            if (symbol == null) {
                continue; // This extension doesn't even have a shortname
            }

            if (Configurator.lookup(e.getClass()).describe().isEmpty()) {
                // There's nothing on can configure on this extension
                // So this useless to expose it - this probably is some technical stuff
                continue;
            }

            attributes.add(new ExtensionAttribute(symbol.value()[0], e.getClass()));
        }

        return attributes;
    }

    @Override
    public String getName() {
        return "jenkins";
    }

    private class ExtensionAttribute extends Attribute {
        public ExtensionAttribute(String name, Class type) {
            super(name, type);
        }

        @Override
        public void setValue(Object target, Object value) throws Exception {
            // nop
        }
    }
}
