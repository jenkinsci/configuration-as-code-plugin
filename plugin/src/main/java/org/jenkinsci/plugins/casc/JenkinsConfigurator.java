package org.jenkinsci.plugins.casc;

import hudson.Extension;
import jenkins.model.Jenkins;
import jenkins.security.s2m.AdminWhitelistRule;
import org.jenkinsci.plugins.casc.model.CNode;
import org.jenkinsci.plugins.casc.model.Mapping;

import javax.annotation.CheckForNull;
import java.util.Set;
import java.util.logging.Logger;

import static org.jenkinsci.plugins.casc.Attribute.NOOP;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension
public class JenkinsConfigurator extends BaseConfigurator<Jenkins> implements RootElementConfigurator<Jenkins> {

    private static final Logger LOGGER = Logger.getLogger(JenkinsConfigurator.class.getName());

    @Override
    public Class<Jenkins> getTarget() {
        return Jenkins.class;
    }

    @Override
    public Jenkins getTargetComponent() {
        return Jenkins.getInstance();
    }

    @Override
    protected Jenkins instance(Mapping mapping) {
        return getTargetComponent();
    }

    @Override
    public Set<Attribute> describe() {
        final Set<Attribute> attributes = super.describe();

        // Add remoting security, all legwork will be done by a configurator
        attributes.add(new Attribute<Jenkins, AdminWhitelistRule>("remotingSecurity", AdminWhitelistRule.class)
                .setter(NOOP)
                .getter(j -> Jenkins.getInstance().getInjector().getInstance(AdminWhitelistRule.class) ));

        return attributes;
    }


    @CheckForNull
    @Override
    public CNode describe(Jenkins instance) throws Exception {

        // we can't generate a fresh new Jenkins object as constructor is mixed with init and check for `theInstance` singleton 
        Mapping mapping = new Mapping();
        for (Attribute attribute : getAttributes()) {
            mapping.put(attribute.getName(), attribute.describe(instance));
        }
        return mapping;
    }

    @Override
    public String getName() {
        return "jenkins";
    }
}
