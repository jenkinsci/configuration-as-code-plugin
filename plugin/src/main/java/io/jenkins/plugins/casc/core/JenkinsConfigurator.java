package io.jenkins.plugins.casc.core;

import hudson.Extension;
import io.jenkins.plugins.casc.Attribute;
import io.jenkins.plugins.casc.BaseConfigurator;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.RootElementConfigurator;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import jenkins.model.Jenkins;
import jenkins.security.s2m.AdminWhitelistRule;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import javax.annotation.CheckForNull;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;

import static io.jenkins.plugins.casc.Attribute.noop;

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
    public Set<Attribute<Jenkins,?>> describe() {
        final Set<Attribute<Jenkins,?>> attributes = super.describe();

        // Add remoting security, all legwork will be done by a configurator
        attributes.add(new Attribute<Jenkins, AdminWhitelistRule>("remotingSecurity", AdminWhitelistRule.class)
                .getter( j -> Jenkins.getInstance().getInjector().getInstance(AdminWhitelistRule.class) )
                .setter( noop() ));

        return attributes;
    }

    @Override
    protected Set<String> exclusions() {
        return Collections.singleton("installState");
    }

    @CheckForNull
    @Override
    public CNode describe(Jenkins instance, ConfigurationContext context) throws Exception {

        // we can't generate a fresh new Jenkins object as constructor is mixed with init and check for `theInstance` singleton 
        Mapping mapping = new Mapping();
        for (Attribute attribute : getAttributes()) {
            mapping.put(attribute.getName(), attribute.describe(instance, context));
        }
        return mapping;
    }

    @Override
    public String getName() {
        return "jenkins";
    }
}
