package org.jenkinsci.plugins.casc.core;

import hudson.Extension;
import jenkins.model.Jenkins;
import jenkins.security.s2m.AdminWhitelistRule;
import org.jenkinsci.plugins.casc.Attribute;
import org.jenkinsci.plugins.casc.BaseConfigurator;
import org.jenkinsci.plugins.casc.ConfigurationContext;
import org.jenkinsci.plugins.casc.RootElementConfigurator;
import org.jenkinsci.plugins.casc.model.CNode;
import org.jenkinsci.plugins.casc.model.Mapping;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import javax.annotation.CheckForNull;
import java.util.Set;
import java.util.logging.Logger;

import static org.jenkinsci.plugins.casc.Attribute.NOOP;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension
@Restricted(NoExternalUse.class)
public class JenkinsConfigurator extends BaseConfigurator<Jenkins> implements RootElementConfigurator<Jenkins> {

    private static final Logger LOGGER = Logger.getLogger(JenkinsConfigurator.class.getName());

    @Override
    public Class<Jenkins> getTarget() {
        return Jenkins.class;
    }

    @Override
    public Jenkins getTargetComponent(ConfigurationContext context) {
        return Jenkins.getInstance();
    }

    @Override
    protected Jenkins instance(Mapping mapping, ConfigurationContext context) {
        return getTargetComponent(context);
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
